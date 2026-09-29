// Crow's Nest icon scene + animation (pack-icon-animation skill). Run inside Blockbench (free format project):
//   eval(require('fs').readFileSync('<this file>', 'utf8')); window.CN = CN; CN.loadTextures()   // then, in a later call:
//   CN.build(); CN.animate(); CN.camera()                                                        // then:
//   CN.render(0, CN.FRAMES - 1)                                                                  // 1600px frames -> frames/
// Other sessions drive the same Blockbench: every entry point re-selects this project (uuid, else name), the global is `CN`,
// and render() holds window.BB_LOCK while it runs and refuses to start under someone else's.
// The gag: a barrel crow's nest on a mast; the copper golem (the channel mascot, rig from Profile-Avatar) peeks over the
// rim with a spyglass at its eye. It dips and pops up, the spyglass telescopes out segment by segment, a thought-bubble
// trail leads to a round zoom bubble, and the tiny creeper on the far hill inside it zooms in (a log-space spring, the
// mod's own zoom curve). The creeper hisses (two white flashes), a "!" pops, the spyglass snaps shut, the bubble poofs,
// the golem ducks into the barrel, peeks, and rises back to the lookout pose.
// Rig (yaw space, golem faces +z; all golem coordinates are its model's plus GY): yaw > root (duck, pop) > squash (feet)
// > arms, neck > head (aim) > nose, eyes, antenna, spyglass (eyepiece, mid, front slide along +z).
// Barrel and mast are static apart from a squash on the duck. FX, dots, bubble and "!" live in `screen` (faces the camera).
// Animated rotations land on mesh.rotation as they are (order ZYX), no negation. Every channel is sampled once per frame
// (linear keys), so renders land exactly on the curves and the loop seam is exact.
var CN = (function () {
  const fs = require('fs');
  const DIR = '/home/emppu/Projects/Minecraft Datapacks/CrowsNest/dev/icon/';
  const TEX = DIR + 'sprites/';
  const PROJECT = { uuid: '9a1634f5-c26a-cbaa-1175-ea124510fbec', name: 'crows_nest_icon' };
  const LOCK_OWNER = 'crows-nest-icon';
  const FPS = 25, DT = 1 / FPS, FRAMES = 80, LEN = FRAMES / FPS;
  let YAW = 25;
  const CAM_POS = [0, 60, 104], CAM_TARGET = [0, 16, 0];
  let CAM_PAN = [0, 0, 0], CAM_ZOOM = 0.5;
  const PITCH = -Math.atan2(CAM_POS[1] - CAM_TARGET[1], CAM_POS[2] - CAM_TARGET[2]) * 180 / Math.PI;
  const O = new THREE.Vector3(...CAM_TARGET);
  const RAD = Math.PI / 180;

  // layout (yaw space, ground at y 0)
  const MAST_H = 10, RIM = MAST_H + 16, GY = RIM - 11;
  const SHOULDER_L = [5.5, 11, 0], ARM_LEN = 9;
  // spyglass eyepiece end on the golem's left eye, the far one: the near (right) eye stays lit and readable
  const EYE = [2, 14, 5];
  const HEAD_PIVOT = [0, 11, 0];
  let AIM = [-18, 26, 0];                   // head rotation looking through the spyglass
  const EXT_MID = 3.5, EXT_FRONT = 5;       // how far the segments slide out
  const BUB = 26;                           // bubble diameter (units)

  const T = {
    dip: 0.24, dipLow: 0.36, pop: 0.44, settle: 0.6,
    ext1: 0.68, ext2: 0.8, glint: 0.88, dot1: 0.92, dot2: 1.0, bubble: 1.08, zoom0: 1.24,
    hiss1: 1.76, hiss2: 1.92, alert: 2.0, retract: 2.04, poof: 2.08, duck: 2.12,
    peek: 2.52, rise: 2.84, blink: 0.12,
  };

  const q = t => Math.round(t * FPS) / FPS;
  const g = v => [v[0], v[1] + GY, v[2]];
  const worldToScreen = w => new THREE.Vector3(...w).sub(O).applyEuler(new THREE.Euler(-PITCH * RAD, 0, 0)).add(O);
  const yawToWorld = a => new THREE.Vector3(...a).applyEuler(new THREE.Euler(0, YAW * RAD, 0));
  const yawToScreen = a => worldToScreen(yawToWorld(a).toArray());

  function own() {
    const p = ModelProject.all.find(p => p.uuid === PROJECT.uuid) || ModelProject.all.find(p => p.name === PROJECT.name);
    if (!p) throw new Error('project ' + PROJECT.name + ' is not open');
    if (Project !== p) p.select();
    return p;
  }

  const tex = {};
  function loadTextures() {
    own();
    Texture.all.slice().forEach(t => t.remove(true));
    for (const f of fs.readdirSync(TEX).filter(f => f.endsWith('.png') && !/^(bg|banner|review)/.test(f))) {
      const url = 'data:image/png;base64,' + fs.readFileSync(TEX + f).toString('base64');
      tex[f.slice(0, -4)] = new Texture({ name: f }).fromDataURL(url).add(false);
    }
    return Object.keys(tex).length + ' textures';
  }
  function ensureTex() {
    if (!Object.keys(tex).length) Texture.all.forEach(t => { tex[t.name.replace('.png', '')] = t; });
  }

  function group(name, origin, parent, rotation) {
    const gr = new Group({ name, origin, rotation: rotation || [0, 0, 0] });
    gr.addTo(parent); gr.init();
    return gr;
  }
  const FACES = ['north', 'south', 'east', 'west', 'up', 'down'];
  // faceTex[f] = [sprite, used width px, used height px, uv rotation, mirror]; UVs are 0..16 across the whole canvas
  function cube(name, from, to, parent, faceTex) {
    const c = new Cube({ name, from, to, box_uv: false });
    c.addTo(parent); c.init();
    for (const f of FACES) {
      const spec = faceTex[f] || faceTex.all;
      if (spec) {
        const t = tex[spec[0]], n = t.width || 16, s = 16 / n;
        const uv = spec[4] ? [spec[1] * s, 0, 0, spec[2] * s] : [0, 0, spec[1] * s, spec[2] * s];
        c.faces[f].extend({ texture: t.uuid, uv, rotation: spec[3] || 0 });
      } else c.faces[f].extend({ texture: null });
    }
    return c;
  }
  // a flat sprite facing +z: [sprite, used w, used h] drawn over w x h units from its bottom-left corner
  function sprite(name, corner, w, h, parent, spec, mirror) {
    const [x, y, z] = corner;
    return cube(name, [x, y, z], [x + w, y + h, z], parent, { south: [...spec, 0, mirror] });
  }

  // golem faces (Profile-Avatar sprites, used part of the 16px canvases)
  const HEAD = { up: ['head_top', 8, 10], south: ['head_front_s', 8, 5], north: ['head_back_s', 8, 5],
    west: ['head_side_w', 10, 5], east: ['head_side_w', 10, 5], down: ['head_bottom', 8, 10] };
  const NOSE = { up: ['nose_top', 2, 2], south: ['nose_front_s', 2, 3], west: ['nose_side_w', 2, 3], east: ['nose_side_w', 2, 3],
    down: ['nose_side_w', 2, 2] };
  const EYES = { south: ['eye', 2, 2] };
  const ROD = { north: ['rod_side_w', 2, 4], south: ['rod_side_w', 2, 4], west: ['rod_side_w', 2, 4], east: ['rod_side_w', 2, 4] };
  const KNOB = { up: ['knob_top', 4, 4], south: ['knob_side_s', 4, 4], north: ['knob_side_s', 4, 4],
    west: ['knob_side_w', 4, 4], east: ['knob_side_w', 4, 4], down: ['knob_side_w', 4, 4] };
  const BODY = { up: ['head_bottom', 8, 6], south: ['body_front_s', 8, 6], north: ['body_back_s', 8, 6],
    west: ['body_side_w', 6, 6], east: ['body_side_w', 6, 6] };
  const ARM = { up: ['arm_top', 3, 4], south: ['arm_front_s', 3, 10], north: ['arm_front_s', 3, 10],
    west: ['arm_side_w', 4, 10], east: ['arm_side_w', 4, 10], down: ['arm_hand', 3, 4] };
  // spyglass segments run along +z: long faces 6 x 4 (up/down rotated), end caps
  const spy = (base, len, w, back, front) => ({
    up: [base, len, w, 90], down: [base + '_w', len, w, 90], west: [base + '_s', len, w], east: [base + '_w', len, w],
    north: back, south: front,
  });

  const G = {};
  const SCR = {};   // screen-space anchors computed at build
  function build() {
    own(); ensureTex();
    Animation.all.slice().forEach(a => a.remove(false));
    Outliner.root.slice().forEach(n => n.remove(false));

    G.yaw = group('yaw', [0, 0, 0], undefined, [0, YAW, 0]);
    cube('shadow_plane', [-9, 0.02, -9], [9, 0.02, 9], G.yaw, { up: ['shadow', 16, 16] });
    cube('mast', [-2, 0, -2], [2, MAST_H, 2], G.yaw, {
      south: ['mast_side_s', 4, MAST_H], west: ['mast_side_w', 4, MAST_H], east: ['mast_side_w', 4, MAST_H], north: ['mast_side_w', 4, MAST_H] });

    // barrel: open-top walls, 1 unit thick; the group squashes about its base
    G.barrel = group('barrel', [0, MAST_H, 0], G.yaw);
    const B0 = MAST_H;
    cube('wall_front', [-8, B0, 7], [8, RIM, 8], G.barrel, {
      south: ['barrel_side_s', 16, 16], north: ['barrel_inner', 16, 16], up: ['barrel_rim', 16, 1],
      west: ['barrel_side_w', 1, 16], east: ['barrel_side_w', 1, 16], down: ['barrel_floor', 16, 1] });
    cube('wall_back', [-8, B0, -8], [8, RIM, -7], G.barrel, {
      south: ['barrel_inner', 16, 16], up: ['barrel_rim', 16, 1], west: ['barrel_side_w', 1, 16], east: ['barrel_side_w', 1, 16] });
    cube('wall_west', [-8, B0, -7], [-7, RIM, 7], G.barrel, {
      west: ['barrel_side_w', 14, 16], east: ['barrel_inner', 14, 16], up: ['barrel_rim', 14, 1, 90], down: ['barrel_floor', 14, 1, 90] });
    cube('wall_east', [7, B0, -7], [8, RIM, 7], G.barrel, {
      east: ['barrel_side_w', 14, 16], west: ['barrel_inner', 14, 16], up: ['barrel_rim', 14, 1, 90] });
    cube('floor', [-7, B0, -7], [7, B0 + 1, 7], G.barrel, { up: ['barrel_floor', 14, 14], down: ['barrel_floor', 14, 14] });
    // flag pole at the back-left corner, the pennant streaming away from the view (-x)
    cube('pole', [-7.8, RIM - 2, -7.8], [-6.8, RIM + 16, -6.8], G.barrel, {
      south: ['pole', 1, 16], west: ['pole_w', 1, 16], east: ['pole_w', 1, 16], north: ['pole_w', 1, 16], up: ['pole', 1, 1] });
    G.flag = group('flag', [-7.3, RIM + 16, -7.3], G.barrel);
    G.flag_0 = group('flag_0', [-7.3, RIM + 16, -7.3], G.flag);
    sprite('flag_0_plane', [-15.3, RIM + 10.4, -7.3], 8, 5.6, G.flag_0, ['flag_0', 10, 7], true);
    G.flag_1 = group('flag_1', [-7.3, RIM + 16, -7.3], G.flag);
    sprite('flag_1_plane', [-15.3, RIM + 10.4, -7.3], 8, 5.6, G.flag_1, ['flag_1', 10, 7], true);

    // the golem (feet at GY, inside the barrel)
    G.root = group('root', [0, GY, 0], G.yaw);
    G.squash = group('squash', [0, GY, 0], G.root);
    cube('body', g([-4, 5, -3]), g([4, 11, 3]), G.squash, BODY);
    G.arm_r = group('arm_r', g([-5.5, 11, 0]), G.squash);
    cube('arm_r_box', g([-7, 2, -2]), g([-4, 12, 2]), G.arm_r, ARM);
    G.arm_l = group('arm_l', g(SHOULDER_L), G.squash);
    cube('arm_l_box', g([4, 2, -2]), g([7, 12, 2]), G.arm_l, ARM);
    G.neck = group('neck', g(HEAD_PIVOT), G.squash);
    G.head = group('head', g(HEAD_PIVOT), G.neck);
    cube('head_box', g([-4, 11, -5]), g([4, 16, 5]), G.head, HEAD);
    G.nose = group('nose', g([0, 11.5, 4]), G.head);
    cube('nose_box', g([-1, 10, 4]), g([1, 13, 6]), G.nose, NOSE);
    G.eye_r = group('eye_r', g([-2, 14, 5]), G.head);
    cube('eye_r_box', g([-3, 13, 5]), g([-1, 15, 5.1]), G.eye_r, EYES);
    G.eye_l = group('eye_l', g([2, 14, 5]), G.head);
    cube('eye_l_box', g([1, 13, 5]), g([3, 15, 5.1]), G.eye_l, EYES);
    G.antenna = group('antenna', g([0, 16, 0]), G.head);
    cube('rod', g([-1, 16, -1]), g([1, 20, 1]), G.antenna, ROD);
    cube('knob', g([-2, 20, -2]), g([2, 24, 2]), G.antenna, KNOB);
    // spyglass on the left eye: eyepiece fixed, mid and front slide out along +z
    const [ex, ey, ez] = EYE;
    G.spy = group('spy', g(EYE), G.head);
    cube('spy_eye', g([ex - 1, ey - 1, ez]), g([ex + 1, ey + 1, ez + 4]), G.spy, spy('spy_eye', 4, 2, ['spy_ring_eye', 2, 2], null));
    G.spy_mid = group('spy_mid', g(EYE), G.spy);
    cube('spy_mid_box', g([ex - 1.5, ey - 1.5, ez + 0.5]), g([ex + 1.5, ey + 1.5, ez + 6.5]), G.spy_mid,
      spy('spy_mid', 6, 3, ['spy_ring_mid', 3, 3], ['spy_ring_mid', 3, 3]));
    G.spy_front = group('spy_front', g(EYE), G.spy);
    cube('spy_front_box', g([ex - 2, ey - 2, ez + 1]), g([ex + 2, ey + 2, ez + 7]), G.spy_front,
      spy('spy_front', 6, 4, ['spy_ring_mid', 3, 3], ['lens', 4, 4]));

    // screen space: everything that faces the camera
    G.screen = group('screen', O.toArray(), undefined, [PITCH, 0, 0]);
    const tip = lensTip(1, 1, 0, AIM), tipS = worldToScreen(tip.toArray());
    const back = worldToScreen(lensTip(0, 0, -15.5, AIM).toArray());
    const dir = new THREE.Vector2(tipS.x - back.x, tipS.y - back.y).normalize();
    // the line of sight bends a little upward so the bubble clears the flag side of the frame
    dir.rotateAround(new THREE.Vector2(0, 0), 36 * RAD);
    const at = d => [tipS.x + dir.x * d, tipS.y + dir.y * d, 60];
    SCR.tip = [tipS.x, tipS.y, 62];
    SCR.dot_s = at(3.5);
    SCR.dot_m = at(7);
    SCR.bubble = at(9 + BUB / 2);
    SCR.alert = (() => { const p = yawToScreen(g([-5, 31, 4])); return [p.x, p.y, 64]; })();
    SCR.rim_l = (() => { const p = yawToScreen([-7, RIM + 1, 7]); return [p.x, p.y, 58]; })();
    SCR.rim_r = (() => { const p = yawToScreen([7, RIM + 1, 7]); return [p.x, p.y, 58]; })();

    const fx = (name, at, size, t, spec) => {
      G[name] = group(name, at, G.screen);
      sprite(name + '_plane', [at[0] - size / 2, at[1] - size / 2, at[2]], size, size, G[name], spec || [t, 16, 16]);
    };
    fx('glint', SCR.tip, 5, 'fx_spark', ['fx_spark', 16, 16]);
    fx('dot_s', SCR.dot_s, 3, 'dot_s', ['dot_s', 4, 4]);
    fx('dot_m', SCR.dot_m, 4.5, 'dot_m', ['dot_m', 6, 6]);
    // bubble: view, creeper (feet on the far hills, row 19.5 of 32), glass ring in front
    const [bx, by, bz] = SCR.bubble;
    G.bubble = group('bubble', SCR.bubble, G.screen);
    sprite('bubble_view', [bx - BUB / 2, by - BUB / 2, bz], BUB, BUB, G.bubble, ['bubble_view', 32, 32]);
    const feet = by + BUB / 2 - 19.6 * BUB / 32;
    const CH = 13, CW = CH * 8 / 18;
    G.creeper = group('creeper', [bx, feet, bz + 0.2], G.bubble);
    G.creeper_n = group('creeper_n', [bx, feet, bz + 0.2], G.creeper);
    sprite('creeper_plane', [bx - CW / 2, feet, bz + 0.2], CW, CH, G.creeper_n, ['creeper_far', 8, 18]);
    G.creeper_f = group('creeper_f', [bx, feet, bz + 0.25], G.creeper);
    sprite('creeper_flash_plane', [bx - CW / 2, feet, bz + 0.25], CW, CH, G.creeper_f, ['creeper_flash', 8, 18]);
    sprite('bubble_ring', [bx - BUB / 2, by - BUB / 2, bz + 0.5], BUB, BUB, G.bubble, ['bubble_ring', 32, 32]);
    fx('poof', [bx, by, bz + 1], 30, 'fx_puff');
    fx('alert', SCR.alert, 7.5, 'fx_alert', ['fx_alert', 4, 10]);
    fx('puff_l', SCR.rim_l, 7, 'fx_puff');
    fx('puff_r', SCR.rim_r, 7, 'fx_puff');
    // the alert sprite is 4 x 10: narrow its plane
    G.alert.children[0].extend({ from: [SCR.alert[0] - 1.5, SCR.alert[1] - 3.75, SCR.alert[2]], to: [SCR.alert[0] + 1.5, SCR.alert[1] + 3.75, SCR.alert[2]] });
    Canvas.updateAll();
    return Outliner.elements.length + ' elements; bubble at ' + SCR.bubble.map(v => v.toFixed(1)) + ', tip ' + SCR.tip.map(v => v.toFixed(1));
  }

  // world position of a point on the spyglass axis (model z offset from the lens end when extended), head rotation `aim`
  function lensTip(eMid, eFront, extra, aim) {
    const zEnd = EYE[2] + 7 + EXT_MID * eMid + EXT_FRONT * eFront + (extra || 0);
    const p = new THREE.Vector3(EYE[0], EYE[1], zEnd).sub(new THREE.Vector3(...HEAD_PIVOT));
    p.applyEuler(new THREE.Euler(aim[0] * RAD, aim[1] * RAD, aim[2] * RAD, 'ZYX'));
    p.add(new THREE.Vector3(...HEAD_PIVOT)).add(new THREE.Vector3(0, GY, 0));
    return yawToWorld(p.toArray());
  }

  // ---- motion curves (t in seconds) ----
  const EASE = { lin: u => u, in: u => u * u, out: u => 1 - (1 - u) * (1 - u), io: u => (u < 0.5 ? 2 * u * u : 1 - 2 * (1 - u) * (1 - u)) };
  const lerp = (a, b, u) => (Array.isArray(a) ? a.map((x, i) => x + (b[i] - x) * u) : a + (b - a) * u);
  function pw(keys) {
    return t => {
      if (t <= keys[0][0] + 1e-9) return keys[0][1];
      for (let i = 1; i < keys.length; i++) {
        const [t1, v1, e] = keys[i], [t0, v0] = keys[i - 1];
        if (t <= t1 + 1e-9) {
          if (e === 'step') return t >= t1 - 1e-9 ? v1 : v0;
          return lerp(v0, v1, EASE[e || 'io']((t - t0) / (t1 - t0)));
        }
      }
      return keys[keys.length - 1][1];
    };
  }
  const clamp01 = u => Math.max(0, Math.min(1, u));
  const win = (t, a, b) => t >= a - 1e-9 && t < b - 1e-9;

  const rootY = pw([
    [0, 0], [T.dip, 0], [T.dipLow, -1.6], [T.pop, 2.2, 'out'], [T.pop + 0.08, -0.5], [T.settle, 0],
    [T.alert - 0.04, 0], [T.alert + 0.04, 1.6, 'out'], [T.duck, -10.5, 'in'], [T.duck + 0.08, -9.2, 'out'], [T.duck + 0.16, -9.8],
    [T.peek, -9.8], [T.peek + 0.12, -2.2, 'out'], [T.peek + 0.2, -2.6], [T.rise, -2.6], [T.rise + 0.12, 0.7, 'out'],
    [T.rise + 0.2, 0], [LEN, 0],
  ]);
  const squashAt = pw([
    [0, [1, 1]], [T.dip, [1, 1]], [T.dipLow, [1.08, 0.9]], [T.pop, [0.93, 1.1], 'out'], [T.pop + 0.08, [1.04, 0.96]], [T.settle, [1, 1]],
    [T.alert - 0.04, [1, 1]], [T.alert, [0.9, 1.14], 'out'], [T.duck, [0.94, 1.08], 'in'], [T.duck + 0.04, [1.12, 0.86], 'lin'],
    [T.duck + 0.12, [0.97, 1.03]], [T.duck + 0.2, [1, 1]],
    [T.rise, [1, 1]], [T.rise + 0.08, [0.94, 1.08], 'out'], [T.rise + 0.16, [1.04, 0.96]], [T.rise + 0.24, [1, 1]], [LEN, [1, 1]],
  ]);
  const headRot = pw([
    [0, AIM], [T.dip, AIM], [T.dipLow, [AIM[0] + 6, AIM[1], 0]], [T.pop, [AIM[0] - 4, AIM[1], 0], 'out'], [T.settle, AIM],
    [T.alert - 0.04, AIM], [T.alert, [AIM[0] - 14, AIM[1] - 6, 4], 'out'], [T.duck, [AIM[0] - 10, AIM[1] - 6, 2]],
    [T.duck + 0.12, [0, 0, 0]],
    // peek: look toward the creeper, then back at the camera, then up into the lookout pose
    [T.peek + 0.12, [0, 10, 0]], [T.peek + 0.2, [0, 34, 0], 'io'], [T.rise - 0.04, [0, 34, 0]], [T.rise + 0.12, AIM, 'out'], [LEN, AIM],
  ]);
  // spyglass: segments slide out with an overshoot, snap shut when startled
  const extKeys = t0 => pw([[0, 0], [t0, 0], [t0 + 0.04, 1.25, 'out'], [t0 + 0.08, 0.9], [t0 + 0.16, 1], [T.retract, 1],
    [T.retract + 0.04, 0, 'in'], [LEN, 0]]);
  const eMid = extKeys(T.ext1), eFront = extKeys(T.ext2);
  // while hiding, the spyglass is held upright against the face so it stays inside the barrel
  const spyRot = pw([[0, [0, 0, 0]], [T.duck - 0.04, [0, 0, 0]], [T.duck + 0.04, [-80, 0, 0], 'out'], [T.rise - 0.04, [-80, 0, 0]],
    [T.rise + 0.12, [0, 0, 0], 'out'], [LEN, [0, 0, 0]]]);

  // the right (near) arm lies over the front rim; folded up inside while hiding, hand on the rim while peeking
  const armRim = pw([[0, [-88, 0, 0]], [T.alert, [-88, 0, 0]], [T.alert + 0.04, [-110, 0, 8], 'out'], [T.duck, [-160, 0, 0]],
    [T.peek, [-160, 0, 0]], [T.peek + 0.12, [-111, 0, 0], 'out'], [T.rise, [-111, 0, 0]], [T.rise + 0.16, [-88, 0, 0]], [LEN, [-88, 0, 0]]]);

  // the left hand holds the eyepiece: aim the arm (hanging along -y) from its shoulder at the grip under it
  let ROT_ORDER = 'ZYX';
  function aimArm(target) {
    const [sx, sy] = SHOULDER_L, dx = target[0] - sx, dy = target[1] - sy;
    const reach = ARM_LEN * ARM_LEN - dx * dx - dy * dy;
    const zs = reach > 0 ? target[2] - Math.sqrt(reach) : target[2] - 0.5;
    const d = new THREE.Vector3(dx, dy, target[2] - zs).normalize();
    const qn = new THREE.Quaternion().setFromUnitVectors(new THREE.Vector3(0, -1, 0), d);
    const e = new THREE.Euler().setFromQuaternion(qn, ROT_ORDER);
    return { pos: [0, 0, zs], rot: [e.x / RAD, e.y / RAD, e.z / RAD] };
  }
  // the grip under the eyepiece, in model coords: spyglass rotation about the eye, then the head's about its pivot
  const grip = (aim, sr) => {
    const e = new THREE.Vector3(...EYE);
    const p = new THREE.Vector3(0, -1.6, 2.5).applyEuler(new THREE.Euler((sr || [0, 0, 0])[0] * RAD, (sr || [0, 0, 0])[1] * RAD, 0, 'ZYX'));
    p.add(e).sub(new THREE.Vector3(...HEAD_PIVOT));
    p.applyEuler(new THREE.Euler(aim[0] * RAD, aim[1] * RAD, aim[2] * RAD, 'ZYX'));
    return p.add(new THREE.Vector3(...HEAD_PIVOT)).toArray();
  };

  const eyeScale = pw([
    [0, [1, 1, 1]], [T.blink - 0.04, [1, 1, 1]], [T.blink, [1, 0.2, 1], 'step'], [T.blink + 0.08, [1, 0.2, 1]],
    [T.blink + 0.12, [1, 1, 1], 'step'],
    [T.alert - 0.04, [1, 1, 1]], [T.alert, [1.6, 1.6, 1], 'out'], [T.duck + 0.08, [1.6, 1.6, 1]],
    [T.peek + 0.12, [1.2, 1.2, 1]], [T.rise, [1.2, 1.2, 1]], [T.rise + 0.12, [1, 1, 1]], [LEN, [1, 1, 1]],
  ]);
  // the rise's wobble is still ringing at the loop point, so it wraps into the start of the next loop
  const antennaAt = t => {
    const wob = (t0, amp) => (t >= t0 && t < t0 + 0.8 ? amp * Math.exp(-(t - t0) / 0.18) * Math.sin(2 * Math.PI * (t - t0) / 0.16) : 0);
    return [wob(T.pop, -10) + wob(T.duck + 0.04, 18) + wob(T.rise + 0.08, -10), 0, wob(T.alert, 12)];
  };
  const antennaRot = t => { const a = antennaAt(t), b = antennaAt(t + LEN); return [a[0] + b[0], 0, a[2] + b[2]]; };
  const barrelSquash = pw([[0, [1, 1, 1]], [T.duck, [1, 1, 1]], [T.duck + 0.04, [1.07, 0.92, 1.07], 'lin'],
    [T.duck + 0.12, [0.97, 1.03, 0.97]], [T.duck + 0.2, [1, 1, 1]], [LEN, [1, 1, 1]]]);

  // the zoom in the bubble: log-space spring from far (0.22) to 1, the mod's own curve (zeta 0.7)
  const FAR = 0.22;
  const zoomSpring = t => {
    if (t < T.zoom0) return FAR;
    const tau = t - T.zoom0, w = 11, z = 0.7, wd = w * Math.sqrt(1 - z * z);
    const y = 1 - Math.exp(-z * w * tau) * (Math.cos(wd * tau) + (z * w / wd) * Math.sin(wd * tau));
    return Math.exp(Math.log(FAR) * (1 - y));
  };
  const hiss = t => win(t, T.hiss1, T.hiss1 + 0.08) || win(t, T.hiss2, T.hiss2 + 0.08);
  const popKeys = (t0, t1, peak) => pw([[0, 0], [t0 - 0.04, 0], [t0, 0.5 * peak, 'step'], [t0 + 0.04, 1.3 * peak, 'out'],
    [t0 + 0.08, 0.92 * peak], [t0 + 0.12, peak], [t1, peak], [t1 + 0.04, 0, 'lin'], [LEN, 0]]);
  function puffKeys(t0, life, peak) {
    return pw([[0, 0], [t0 - 0.04, 0], [t0, 0.45 * peak, 'step'], [t0 + 0.08, peak, 'out'], [t0 + life, peak * 1.15],
      [t0 + life + 0.04, 0, 'lin'], [LEN, 0]]);
  }

  // ---- animation ----
  let A = null;
  function K(gr, ch, t, v) {
    const [x, y, z] = typeof v === 'number' ? [v, v, v] : v;
    A.getBoneAnimator(gr).addKeyframe({ channel: ch, time: q(t), interpolation: 'linear', data_points: [{ x, y, z }] });
  }
  function sampled(gr, ch, f) {
    const v = Array.from({ length: FRAMES + 1 }, (_, i) => {
      const r = f(i * DT);
      return typeof r === 'number' ? [r, r, r] : r;
    });
    const same = (a, b) => a.every((x, i) => Math.abs(x - b[i]) < 1e-6);
    v.forEach((val, i) => {
      if (i > 0 && i < FRAMES && same(val, v[i - 1]) && same(val, v[i + 1])) return;
      K(gr, ch, i * DT, val);
    });
  }

  function ensureG() {
    if (!G.yaw) Group.all.forEach(gr => { G[gr.name] = gr; });
  }
  function animate() {
    own(); ensureTex(); ensureG();
    if (!SCR.bubble) return 'run build() first (screen anchors)';
    Animation.all.slice().forEach(a => a.remove(false));
    A = new Animation({ name: 'icon_loop', length: LEN, loop: 'loop', snapping: FPS }).add(false);
    A.select();
    ROT_ORDER = G.arm_l.mesh.rotation.order;

    sampled(G.root, 'position', t => [0, rootY(t), 0]);
    sampled(G.squash, 'scale', t => { const [a, b] = squashAt(t); return [a, b, a]; });
    sampled(G.head, 'rotation', headRot);
    sampled(G.eye_r, 'scale', eyeScale);
    sampled(G.antenna, 'rotation', antennaRot);
    sampled(G.spy_mid, 'position', t => [0, 0, EXT_MID * eMid(t)]);
    sampled(G.spy_front, 'position', t => [0, 0, EXT_MID * eMid(t) + EXT_FRONT * eFront(t)]);
    sampled(G.spy, 'rotation', spyRot);
    // the left hand holds the spyglass, except while hiding: then the arm hangs inside the barrel
    const hold = pw([[0, 1], [T.duck - 0.04, 1], [T.duck + 0.04, 0, 'out'], [T.rise - 0.04, 0], [T.rise + 0.12, 1, 'out'], [LEN, 1]]);
    const HANG = { pos: [0, 0, 0], rot: [-160, 0, 0] };   // folded up inside, clear of the floor and the walls
    const armHold = t => { const a = aimArm(grip(headRot(t), spyRot(t))), w = hold(t); return { pos: lerp(HANG.pos, a.pos, w), rot: lerp(HANG.rot, a.rot, w) }; };
    sampled(G.arm_l, 'rotation', t => armHold(t).rot);
    sampled(G.arm_l, 'position', t => armHold(t).pos);
    sampled(G.arm_r, 'rotation', armRim);
    sampled(G.barrel, 'scale', barrelSquash);
    // pennant flutter: two frames, 0.16 s each (20 swaps a loop, so the seam is exact)
    sampled(G.flag_0, 'scale', t => (Math.floor(t / 0.16 + 1e-6) % 2 === 0 ? 1 : 0));
    sampled(G.flag_1, 'scale', t => (Math.floor(t / 0.16 + 1e-6) % 2 === 1 ? 1 : 0));

    sampled(G.glint, 'scale', pw([[0, 0], [T.glint - 0.04, 0], [T.glint, 0.9, 'step'], [T.glint + 0.04, 1.4, 'out'], [T.glint + 0.08, 0.8],
      [T.glint + 0.12, 0, 'lin'], [LEN, 0]]));
    sampled(G.glint, 'rotation', pw([[0, [0, 0, 0]], [T.glint, [0, 0, 0]], [T.glint + 0.12, [0, 0, 45], 'lin'], [LEN, [0, 0, 45]]]));
    sampled(G.dot_s, 'scale', popKeys(T.dot1, T.poof - 0.04, 1));
    sampled(G.dot_m, 'scale', popKeys(T.dot2, T.poof, 1));
    sampled(G.bubble, 'scale', pw([[0, 0], [T.bubble - 0.04, 0], [T.bubble, 0.4, 'step'], [T.bubble + 0.08, 1.16, 'out'],
      [T.bubble + 0.12, 0.95], [T.bubble + 0.16, 1], [T.poof - 0.04, 1], [T.poof, 1.1, 'out'], [T.poof + 0.04, 0, 'lin'], [LEN, 0]]));
    sampled(G.creeper, 'scale', t => {
      const s = zoomSpring(t) * (hiss(t) ? 1.07 : 1);
      return [s, s, 1];
    });
    sampled(G.creeper_n, 'scale', t => (hiss(t) ? 0 : 1));
    sampled(G.creeper_f, 'scale', t => (hiss(t) ? 1 : 0));
    sampled(G.poof, 'scale', puffKeys(T.poof, 0.16, 1));
    sampled(G.poof, 'rotation', t => [0, 0, t > T.poof ? (t - T.poof) * 90 : 0]);
    sampled(G.alert, 'scale', popKeys(T.alert, T.alert + 0.4, 1));
    sampled(G.puff_l, 'scale', puffKeys(T.duck + 0.04, 0.12, 0.8));
    sampled(G.puff_r, 'scale', puffKeys(T.duck + 0.04, 0.12, 0.8));
    const rise = (t0, dx) => t => { const u = EASE.out(clamp01((t - t0) / 0.24)); return [dx * u, 2 * u, 0]; };
    sampled(G.puff_l, 'position', rise(T.duck + 0.04, -2));
    sampled(G.puff_r, 'position', rise(T.duck + 0.04, 2));

    Animator.preview();
    const warn = [];
    // the loop seam: every sampled channel at 0 and LEN must agree
    ['rootY', 'squashAt', 'headRot', 'eyeScale', 'eMid', 'eFront', 'antennaRot', 'spyRot', 'armRim'].forEach(n => {
      const f = { rootY, squashAt, headRot, eyeScale, eMid, eFront, antennaRot, spyRot, armRim }[n];
      if (JSON.stringify(f(0).toString()) !== JSON.stringify(f(LEN).toString()) && Math.max(...[].concat(f(0)).map((v, i) => Math.abs(v - [].concat(f(LEN))[i]))) > 1e-6) warn.push(n + ' differs at the seam');
    });
    return 'animated' + (warn.length ? '; ' + warn.join(', ') : '');
  }

  // ---- checks ----
  // the spyglass lens centre (world) at time t vs the analytic tip used to place the bubble
  function tipCheck(t) {
    own(); ensureG();
    setTime(t);
    const lens = G.spy_front.mesh.localToWorld(new THREE.Vector3(EYE[0], EYE[1] + GY, EYE[2] + 7).sub(new THREE.Vector3(...g(EYE))));
    const hand = G.arm_l.mesh.localToWorld(new THREE.Vector3(0, -ARM_LEN, 0));
    const gp = G.head.mesh.localToWorld(new THREE.Vector3(...g([EYE[0], EYE[1] - 1.6, EYE[2] + 2.5])).sub(new THREE.Vector3(...g(HEAD_PIVOT))));
    return { lens: lens.toArray().map(v => +v.toFixed(2)), analytic: lensTip(eMid(t), eFront(t), 0, headRot(t)).toArray().map(v => +v.toFixed(2)),
      handToGrip: +hand.distanceTo(gp).toFixed(2) };
  }

  // ---- camera + render ----
  function camera(zoom, pan) {
    own();
    const p = Preview.selected;
    p.setProjectionMode(true);
    const pp = pan || CAM_PAN;
    const pos = CAM_POS.map((v, i) => v + pp[i]), tgt = CAM_TARGET.map((v, i) => v + pp[i]);
    p.camera.position.set(...pos);
    p.controls.target.set(...tgt);
    p.camera.lookAt(...tgt);
    p.camera.zoom = zoom || CAM_ZOOM; p.camera.updateProjectionMatrix();
    p.controls.update();
  }
  function setView(zoom, pan) { if (zoom) CAM_ZOOM = zoom; if (pan) CAM_PAN = pan; }
  // try poses: call before build() (the curves read AIM and YAW when they are made, so re-eval the script to reset)
  function setPose(aim, yaw) { if (aim) AIM = aim; if (yaw !== undefined) YAW = yaw; }
  function setTime(t) {
    Timeline.setTime(t);
    Animator.preview();
  }
  function render(first, last, res, dir) {
    const lock = window.BB_LOCK;
    if (lock && lock.owner !== LOCK_OWNER && lock.until > Date.now()) return 'locked by ' + lock.owner + ' for ' + Math.round((lock.until - Date.now()) / 1000) + ' s';
    window.BB_LOCK = { owner: LOCK_OWNER, until: Date.now() + 180000 };
    own();
    res = res || 1600;
    dir = dir || DIR + 'frames/';
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    const shot = f => new Promise(done => {
      setTime(f * DT);
      Screencam.advancedScreenshot(Preview.selected, { angle_preset: 'view', resolution: [res, res], anti_aliasing: 'none', shading: false }, url => {
        fs.writeFileSync(dir + 'frame_' + String(f).padStart(3, '0') + '.png', Buffer.from(url.split(',')[1], 'base64'));
        done();
      });
    });
    return (async () => {
      try { for (let f = first; f <= last; f++) await shot(f); } finally { window.BB_LOCK = null; }
      return `rendered ${first}..${last} into ${dir}`;
    })();
  }
  function scaleSweep() {
    own();
    const bad = [];
    for (let f = 0; f <= FRAMES; f++) {
      setTime(f * DT);
      Group.all.forEach(gr => { const s = gr.mesh.scale; if (s.x < 0 || s.y < 0 || s.z < 0) bad.push(gr.name + '@' + f); });
    }
    return bad.length ? bad.join(',') : 'no negative scale';
  }
  function save() {
    own();
    Codecs.project.write(Codecs.project.compile(), DIR + 'crows_nest_icon.bbmodel');
    return 'saved';
  }

  return { FPS, DT, FRAMES, LEN, T, PITCH, G, SCR, tex, own, loadTextures, build, animate, camera, setView, setPose, render, setTime,
    scaleSweep, tipCheck, save, worldToScreen, yawToWorld, yawToScreen, q };
})();
