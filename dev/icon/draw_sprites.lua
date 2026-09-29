-- Far Out Zoom icon sprites (pack-icon-animation skill). Run through the aseprite MCP:
--   dofile("/home/emppu/Projects/Minecraft Datapacks/FarOut/dev/icon/draw_sprites.lua")
-- The icon is the view through a scope; the scope rim, sky, hills and speed lines are drawn procedurally by
-- dev/make_icon.py at every frame (so the zoom stays pixel-crisp). This script draws the sprites it places: the creeper
-- (8 x 26 front view, drawn from scratch; vanilla's was only a shape reference) and its hiss flash, the "!", the far
-- explosion poof, the backgrounds and the lettering. fx_puff and fx_spark are Veinminer's (copied via dev/icon/v1).
dofile("/home/emppu/Projects/Minecraft Datapacks/.claude/skills/pack-icon-animation/assets/pixel_art.lua")
local OUT = "/home/emppu/Projects/Minecraft Datapacks/FarOut/dev/icon/sprites/"

local CREEPER = { ["1"] = "#0F2E14", ["2"] = "#1E5A26", ["3"] = "#2E8436", ["4"] = "#45AD45", ["5"] = "#6ED163", ["6"] = "#A9EE8E", k = "#0B120C" }
PA.sprite_from_grid(OUT .. "creeper", {
  "56655665",
  "54455445",
  "4kk44kk4",
  "4kk54kk5",
  "544kk445",
  "44kkkk44",
  "54kkkk45",
  "34k44k43",
  "45545545",
  "44545444",
  "54454354",
  "45435445",
  "44544534",
  "54354445",
  "45445435",
  "34543445",
  "45434545",
  "44354434",
  "34443443",
  "33433343",
  "4543.454",
  "4435.343",
  "3543.435",
  "3433.343",
  "2332.233",
  "2222.222",
}, CREEPER)
PA.whiten(OUT .. "creeper.aseprite", OUT .. "creeper_flash", 0.75)

PA.sprite_from_grid(OUT .. "fx_alert", {
  ".YY.",
  "WYYO",
  "WYYO",
  "WYYO",
  ".YO.",
  ".YO.",
  "....",
  ".YY.",
  ".YO.",
}, { W = "#FFF7D6", Y = "#FFD45C", O = "#E0801E" })

-- the far explosion: a tiny cartoon boom (7 x 7) and its cooling smoke
PA.sprite_from_grid(OUT .. "boom", {
  "..Y.Y..",
  ".YWWWY.",
  "YWWOWWY",
  ".WOROW.",
  "YWWOWWY",
  ".YWWWY.",
  "..Y.Y..",
}, { W = "#FFF7D6", Y = "#FFD45C", O = "#FF9A2E", R = "#D9621E" })

-- backgrounds: flat plum, 64x64 (icon) and 192x64 (banner, a few twinkles)
local PLUM = "#4A2045"
local function flat(name, w, h, dots)
  local px = {}
  for y = 0, h - 1 do for x = 0, w - 1 do px[PA.key(x, y)] = PLUM end end
  for _, d in ipairs(dots or {}) do px[PA.key(d[1], d[2])] = d[3] end
  PA.save_pixels(OUT .. name, w, h, px)
end
flat("bg_flat", 64, 64)
local dots = {}
for n, p in ipairs({ { 9, 8 }, { 27, 17 }, { 44, 6 }, { 61, 12 }, { 80, 5 }, { 96, 9 }, { 104, 21 }, { 118, 4 }, { 131, 14 }, { 150, 7 },
  { 163, 18 }, { 176, 5 }, { 186, 12 }, { 14, 52 }, { 38, 58 }, { 60, 49 }, { 183, 55 }, { 170, 46 } }) do
  dots[#dots + 1] = { p[1], p[2], n % 3 == 0 and "#9A5A8E" or "#6A3462" }
end
flat("banner_bg", 192, 64, dots)

-- lettering: FAR OUT in sunset bands, ZOOM in the scope's sky blues (a 70s "far out" glow), plum extrude, tagline
local SUNSET = { "#FFF3B0", "#FFE066", "#FFE066", "#FFC24D", "#FFA640", "#FFA640", "#FF8A3D", "#FF6B4A", "#F2555A", "#F2555A" }
PA.title_sprite(OUT .. "banner_title", "FAR OUT", { bands = SUNSET, extrude = { "#2E1029", "#200A1C" }, outline = "#16060F" })
local SKY = { "#F2FAFF", "#CBE9FF", "#CBE9FF", "#A3D6F8", "#A3D6F8", "#7EC3F2", "#7EC3F2", "#5FAFE8", "#4A98D6", "#4A98D6" }
PA.title_sprite(OUT .. "banner_title_zoom", "ZOOM", { bands = SKY, extrude = { "#2E1029", "#200A1C" }, outline = "#16060F" })
PA.SMALL[","] = {"..","..","..","..",".#",".#","#."}   -- a taller comma, the kit one reads as a full stop at x6
PA.label_sprite(OUT .. "banner_tagline", "THE HORIZON, UP CLOSE.", function(i) return i > 13 and "#FFC24D" or "#FFFFFF" end, "#16060F")

-- Description Kit pieces: spyglass (centre piece), creeper face (caps), brass strip (the scope ring's ramp, notched)
local BRASS = { o = "#16060F", a = "#FFF0BE", b = "#F0CE78", c = "#D5A444", d = "#AA7626", e = "#7C5018",
                l = "#E8F7FF", m = "#7EC3F2", n = "#3F7FB8" }
PA.sprite_from_grid(OUT .. "spyglass_item", {
  "............o...",
  "...........olo..",
  "..........oalmo.",
  ".........oabcmno",
  "........odbcdeo.",
  ".......oadedeo..",
  "......oabceeo...",
  ".....oabcdeo....",
  ".....odcdeo.....",
  "....oaeeeo......",
  "...oacdoo.......",
  "..oacdo.........",
  ".oacdo..........",
  ".ocdo...........",
  "..oo............",
  "................",
}, BRASS)
PA.sprite_from_grid(OUT .. "creeper_face", {
  "oooooooooo",
  "o56655665o",
  "o54455445o",
  "o4kk44kk4o",
  "o4kk54kk5o",
  "o544kk445o",
  "o44kkkk44o",
  "o54kkkk45o",
  "o34k44k43o",
  "oooooooooo",
}, { o = "#16060F", ["3"] = "#2E8436", ["4"] = "#45AD45", ["5"] = "#6ED163", ["6"] = "#A9EE8E", k = "#0B120C" })
PA.sprite_from_grid(OUT .. "brass_strip", {
  "aaaaaaaaaaaaaaaa",
  "bbbbbbbbbbbbbbbb",
  "cccecccccccecccc",
  "dddedddddddedddd",
  "eeeeeeeeeeeeeeee",
}, BRASS)
