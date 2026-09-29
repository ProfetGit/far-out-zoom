-- Crow's Nest icon sprites (pack-icon-animation skill). Run through the aseprite MCP:
--   dofile("/home/emppu/Projects/Minecraft Datapacks/CrowsNest/dev/icon/draw_sprites.lua")
-- The copper golem (head, nose, rod, knob, body, arms, eye), fx_puff, fx_spark and the shadow key disc are copied from
-- Profile-Avatar/dev/icon/sprites (same author, the channel mascot). Everything here is drawn from scratch; vanilla's
-- barrel, spyglass and creeper were only shape references.
-- Face textures sit in the top-left of their canvas; build_scene.js sets each face's UV to the used part. The render uses
-- shading:false, so faces are pre-shaded like the golem's: up = as drawn, south (_s) 1 step darker, east/west (_w) 2 steps.
dofile("/home/emppu/Projects/Minecraft Datapacks/.claude/skills/pack-icon-animation/assets/pixel_art.lua")
local OUT = "/home/emppu/Projects/Minecraft Datapacks/CrowsNest/dev/icon/sprites/"

-- spruce-brown barrel wood (browner and cooler than the golem's copper), iron bands
local WOOD = { ["1"] = "#24160E", ["2"] = "#3C2616", ["3"] = "#5A3A20", ["4"] = "#7A512D", ["5"] = "#9A6B3E", ["6"] = "#B98A55", ["7"] = "#D3A870" }
local IRON = { i = "#23253A", j = "#3E4259", k = "#636A88", l = "#99A0BD" }
-- grey-brown oak bark for the mast
local BARK = { m = "#33261A", n = "#4E3B28", o = "#6D5539", p = "#8C704E", q = "#AB8E66" }
-- the golem's copper ramp (spyglass tube) and amethyst (lens)
local COPPER = { ["0"] = "#2A0F0B", ["1"] = "#4A1C12", ["2"] = "#74301C", ["3"] = "#A04626", ["4"] = "#C9622F",
  ["5"] = "#E7834A", ["6"] = "#F7A96A", ["7"] = "#FFD39C", ["8"] = "#FFF0D6" }
local AMETHYST = { a = "#2A1248", b = "#4B2682", c = "#7A4FC4", d = "#A77BEB", e = "#DCC6FF" }
-- creeper greens and its black face
local CREEPER = { ["1"] = "#0F2E14", ["2"] = "#1E5A26", ["3"] = "#2E8436", ["4"] = "#45AD45", ["5"] = "#6ED163", ["6"] = "#A9EE8E", k = "#0B120C" }
local FLAG = { R = "#D8322E", r = "#FF6B5A", d = "#A51F24" }

local function merge(...)
  local out = {}
  for _, t in ipairs({ ... }) do for k, v in pairs(t) do out[k] = v end end
  return out
end
-- a colour map that shifts every ramp n steps darker (ramps given as {table, "order light->dark reversed"})
local function darker(ramps, n)
  local m = {}
  for _, r in ipairs(ramps) do
    local t, order = r[1], r[2]
    for i = 1, #order do m[t[order:sub(i, i)]] = t[order:sub(math.max(1, i - n), math.max(1, i - n))] end
  end
  return m
end
-- faces go in the top-left of a square canvas (16 unless given): Blockbench treats taller-than-wide images as animated
-- strips, and build_scene.js picks the used part with the face UV
local function pad(rows, n)
  local out = {}
  for y = 1, n do out[y] = (rows[y] or "") .. string.rep(".", n - #(rows[y] or "")) end
  return out
end
local function face(name, rows, legend, shades, n)
  PA.sprite_from_grid(OUT .. name, pad(rows, n or 16), legend)
  for suffix, map in pairs(shades or {}) do PA.remap(OUT .. name .. ".aseprite", OUT .. name .. suffix, map) end
end

-- barrel crow's nest: 16 x 16 walls (a full barrel block), 1 unit thick, open top ---------------------------------------------------
local WOOD_R = { WOOD, "1234567" }
local IRON_R = { IRON, "ijkl" }
local BARK_R = { BARK, "mnopq" }
local COPPER_R = { COPPER, "012345678" }
local barrelShades = { _s = darker({ WOOD_R, IRON_R }, 1), _w = darker({ WOOD_R, IRON_R }, 2) }
face("barrel_side", {
  "7777777777777777",
  "6554655465546554",
  "6554654465546554",
  "llkklkkkkkklkkkl",
  "jjjjjjjjjjjjjjjj",
  "6554655465446554",
  "6545655465546554",
  "6554655464546554",
  "6554654465546554",
  "6554655465546545",
  "6554655465546554",
  "llkklkkkkkklkkkl",
  "jjjjjjjjjjjjjjjj",
  "6554655465546554",
  "6554654465546554",
  "4443444344434443",
}, merge(WOOD, IRON), barrelShades)
-- inside walls, seen over the rim: in shadow
face("barrel_inner", {
  "3332333233323332",
  "3221322132213221",
  "3221321132213221",
  "jjiijiiiiiijiiij",
  "iiiiiiiiiiiiiiii",
  "3221321132213221",
  "3221322132113221",
  "3212322132213221",
  "3221322131213221",
  "3221322132213212",
  "3221322132213221",
  "jjiijiiiiiijiiij",
  "iiiiiiiiiiiiiiii",
  "3221322132213221",
  "3221321132213221",
  "2221222122212221",
}, merge(WOOD, IRON))
-- the top of each wall (end grain), 16 x 1
face("barrel_rim", { "7666666666666667" }, WOOD)
face("barrel_floor", {
  "2222222222222222", "2111211121112111", "2111211121112111", "2111211121112111",
  "2222222222222222", "2111211121112111", "2111211121112111", "2111211121112111",
  "2222222222222222", "2111211121112111", "2111211121112111", "2111211121112111",
  "2222222222222222", "2111211121112111", "2111211121112111", "2111211121112111",
}, WOOD)

-- mast: 4 x 4 post, bark with vertical grain; flag pole: 1 x 1 iron
local barkShades = { _s = darker({ BARK_R }, 1), _w = darker({ BARK_R }, 2) }
face("mast_side", {
  "qpon", "qpnn", "qpon", "qmon", "qpon", "qpon", "qpmn", "qpon",
  "qpon", "qmon", "qpon", "qpon", "qpnn", "qpon", "qpmn", "qpon",
}, BARK, barkShades)
face("mast_top", { "qppo", "popo", "pooo", "onnn" }, BARK)
face("pole", { "l", "k", "k", "k", "k", "k", "k", "k", "k", "k", "k", "k", "k", "k", "k", "j" }, IRON)
face("pole_w", { "k", "j", "j", "j", "j", "j", "j", "j", "j", "j", "j", "j", "j", "j", "j", "i" }, IRON)

-- pennant, two flutter frames (10 x 7)
face("flag_0", {
  "rR........",
  "rRRRR.....",
  "rRRRRRRR..",
  "rRRRRRRRRR",
  "dRRRRRRR..",
  "dRRdd.....",
  "dd........",
}, FLAG)
face("flag_1", {
  "rR........",
  "rRRR......",
  "rRRRRRR...",
  "rRRRRRRRR.",
  "dRRRRRRRRR",
  "dRRRRd....",
  "dd........",
}, FLAG)

-- spyglass: three telescoping copper segments along +x; the lens (amethyst) caps the widest one --------------------------
local spyShades = { _s = darker({ COPPER_R }, 1), _w = darker({ COPPER_R }, 2) }
-- front (lens) segment: 4 around x 6 long, a darker ring at each end
face("spy_front", { "577775", "466664", "355553", "244442" }, COPPER, spyShades)
-- middle: 3 around x 6 long
face("spy_mid", { "577775", "466664", "244442" }, COPPER, spyShades)
-- eyepiece: 2 around x 4 long, dark leather-brown grip
face("spy_eye", { "3443", "2332" }, COPPER, spyShades)
face("spy_ring_mid", { "343", "424", "343" }, COPPER)
face("spy_ring_eye", { "21", "10" }, COPPER)
face("lens", { "bccb", "cedc", "cddc", "bccb" }, AMETHYST)
PA.whiten(OUT .. "lens.aseprite", OUT .. "lens_glint", 0.65)

-- zoom bubble: the magnified view (32 x 32) and its glass ring ------------------------------------------------------------
do
  local px, c = {}, 15.5
  local SKY = { "#6DB8F0", "#8CCBF7", "#AEDCFB", "#D2EDFF" }
  for y = 0, 31 do for x = 0, 31 do
    local dx, dy = x - c, y - c
    if dx * dx + dy * dy <= 14.2 * 14.2 then
      local col
      -- rolling far hills: horizon around row 19
      local hill = 19 + math.floor(1.6 * math.sin(x * 0.42) + 0.8 * math.sin(x * 0.9 + 1))
      if y >= hill then
        col = (y == hill) and "#7CCB64" or (y <= hill + 2 and "#5AAE4E" or (y <= hill + 5 and "#4A9A44" or "#3D843C"))
      else
        col = SKY[math.min(4, 1 + (y // 5))]
      end
      px[PA.key(x, y)] = col
    end
  end end
  -- a small cloud upper left, and a glare arc on the glass
  for _, p in ipairs({ { 7, 8 }, { 8, 7 }, { 9, 7 }, { 10, 7 }, { 8, 8 }, { 9, 8 }, { 10, 8 }, { 11, 8 }, { 6, 9 }, { 7, 9 }, { 8, 9 }, { 9, 9 }, { 10, 9 }, { 11, 9 }, { 12, 9 } }) do
    px[PA.key(p[1], p[2])] = (p[2] == 9) and "#E6F4FF" or "#FFFFFF"
  end
  for a = 200, 245, 3 do
    local r = math.rad(a)
    px[PA.key(math.floor(c + 11.3 * math.cos(r) + 0.5), math.floor(c + 11.3 * math.sin(r) + 0.5))] = "#FFFFFF"
  end
  PA.save_pixels(OUT .. "bubble_view", 32, 32, px)
  local ring = {}
  for y = 0, 31 do for x = 0, 31 do
    local dx, dy = x - c, y - c
    local r = math.sqrt(dx * dx + dy * dy)
    if r > 13.6 and r <= 15.9 then
      -- glass rim: white, a cool shade on the inner edge of the lower right
      ring[PA.key(x, y)] = (r < 14.6 and dx + dy > 4) and "#B9D8F0" or "#FFFFFF"
    end
  end end
  PA.save_pixels(OUT .. "bubble_ring", 32, 32, ring)
end
-- thought-bubble trail from the lens to the bubble
local DOT = { w = "#FFFFFF", s = "#B9D8F0" }
face("dot_s", { ".ww.", "wwww", "wwws", ".ss." }, DOT)
face("dot_m", { "..ww..", ".wwwww", "wwwwww", "wwwwws", ".wwwss", "..ss.." }, DOT)

-- the creeper on the far hill (8 x 18, front view): head, body, two front feet
face("creeper_far", {
  "56545565",
  "54455445",
  "4kk44kk4",
  "4kk54kk5",
  "544kk445",
  "44kkkk44",
  "54kkkk45",
  "44k44k44",
  ".344543.",
  ".435434.",
  ".344543.",
  ".453443.",
  ".344534.",
  ".234432.",
  "343..343",
  "343..343",
  "232..232",
  "222..222",
}, CREEPER, nil, 18)
-- about to go off: the hiss flash
PA.whiten(OUT .. "creeper_far.aseprite", OUT .. "creeper_flash", 0.7)

-- "!" over the golem when it sees the creeper (4 x 10)
face("fx_alert", {
  ".YY.",
  "WYYO",
  "WYYO",
  "WYYO",
  ".YO.",
  ".YO.",
  "....",
  ".YY.",
  ".YO.",
  "....",
}, { W = "#FFF7D6", Y = "#FFD45C", O = "#E0801E" })

-- backgrounds: flat ocean blue, 64x64 (icon) and 192x64 (banner) cells; the banner gets a few twinkles --------------------
local OCEAN = "#23507E"
local function flat(name, w, h, dots)
  local px = {}
  for y = 0, h - 1 do for x = 0, w - 1 do px[PA.key(x, y)] = OCEAN end end
  for _, d in ipairs(dots or {}) do px[PA.key(d[1], d[2])] = d[3] end
  PA.save_pixels(OUT .. name, w, h, px)
end
flat("bg_flat", 64, 64)
local dots = {}
for n, p in ipairs({ { 9, 8 }, { 27, 17 }, { 44, 6 }, { 61, 12 }, { 80, 5 }, { 96, 9 }, { 104, 21 }, { 118, 4 }, { 131, 14 }, { 150, 7 },
  { 163, 18 }, { 176, 5 }, { 186, 12 }, { 14, 52 }, { 38, 58 }, { 60, 49 }, { 183, 55 }, { 170, 46 } }) do
  dots[#dots + 1] = { p[1], p[2], n % 3 == 0 and "#6E9CCB" or "#3B6A9A" }
end
flat("banner_bg", 192, 64, dots)

-- lettering: CROW'S in cream, NEST in the spyglass's copper, deep-sea extrude; tagline -----------------------------------
local CREAM = { "#FFFFFF", "#FFF7E6", "#FFF7E6", "#FFF7E6", "#F2E2C4", "#F2E2C4", "#F2E2C4", "#DCC39B", "#DCC39B", "#DCC39B" }
local COPPER_BANDS = { "#FFF0D6", "#FFD39C", "#FFD39C", "#FFD39C", "#F7A96A", "#F7A96A", "#F7A96A", "#E7834A", "#E7834A", "#E7834A" }
local EXTRUDE = { "#163A5E", "#0F2A45" }
local INK = "#0A1A2C"
local N8 = { { -1, -1 }, { 0, -1 }, { 1, -1 }, { -1, 0 }, { 1, 0 }, { -1, 1 }, { 0, 1 }, { 1, 1 } }
-- a one-line title whose first `split` characters use bandsA and the rest bandsB
local function two_tone_title(path, text, split, bandsA, bandsB)
  local mask, right = PA.layout(text, PA.TITLE, 2, 4, 1, 1)
  local px = {}
  for k, v in pairs(mask) do px[k] = (v[2] <= split and bandsA or bandsB)[v[1]] end
  for k in pairs(mask) do
    local x, y = k % 4096, k // 4096
    for d, col in ipairs(EXTRUDE) do
      local n = PA.key(x, y + d)
      if not mask[n] and not px[n] then px[n] = col end
    end
  end
  local solid = {}
  for k in pairs(px) do solid[k] = true end
  for k in pairs(solid) do
    local x, y = k % 4096, k // 4096
    for _, d in ipairs(N8) do
      local n = PA.key(x + d[1], y + d[2])
      if not solid[n] then px[n] = INK end
    end
  end
  PA.save_pixels(path, right + 1, 10 + #EXTRUDE + 2, px)
end
two_tone_title(OUT .. "banner_title", "CROW'S NEST", 6, CREAM, COPPER_BANDS)
PA.label_sprite(OUT .. "banner_tagline", "HOLD C. SEE FAR.", function(i) return i > 8 and "#FFD39C" or "#FFFFFF" end, INK)
