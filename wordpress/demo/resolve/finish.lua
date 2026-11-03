-- Finishing the demo video in DaVinci Resolve (D293): what FCPXML cannot carry, applied the same
-- way every time. `node bin/story-video.mjs --language <lang> --project` writes this file into
-- story.<lang>.project/ with its paths filled in; nothing here needs editing.
--
-- Lua, not Python: Resolve runs Lua with nothing installed, while a Python script is listed only
-- when Resolve finds a Python of its own (the Microsoft Store one it does not). Resolve's free
-- version runs scripts only from inside the app: copy this file into
-- %APPDATA%\Blackmagic Design\DaVinci Resolve\Support\Fusion\Scripts\Utility\, restart Resolve
-- and pick it under Workspace > Scripts, with Workspace > Console open to read what it says.
--
-- It creates (or opens) a project named for the video, imports the timeline, says whether any
-- clip came in offline (D290's check), adds the narration's captions as a subtitle track,
-- applies the Fairlight preset named below when one is saved, and queues a YouTube render. It
-- starts no render and changes nothing outside that project.

local NAME = "__NAME__"
local FCPXML = [[__FCPXML__]]
local SRT = [[__SRT__]]
local RENDER_DIR = [[__RENDER_DIR__]]
-- Save the dialogue chain (EQ, de-esser, Dialogue Leveler) once as a Fairlight preset with this
-- name, and every run applies it.
local FAIRLIGHT_PRESET = "Viglet story voice"

-- Also written to finish.log beside the project: a script picked from the Scripts menu prints
-- nowhere a person can read.
-- Opened inside pcall: where Resolve's Lua has no `io`, an unguarded open ended the script before
-- its first line, with nothing said anywhere. Fusion's own `bmd.writefile` is the fallback.
local LOG = RENDER_DIR .. "\\finish.log"
local logged = {}
local ok_io, log = pcall(function()
	return io.open(LOG, "w")
end)
if not ok_io then
	log = nil
end
local function say(text)
	print("finish: " .. text)
	table.insert(logged, text)
	if log then
		pcall(function()
			log:write(text .. "\n")
			log:flush()
		end)
	elseif bmd and bmd.writefile then
		pcall(bmd.writefile, LOG, table.concat(logged, "\n") .. "\n")
	end
end

-- Everything below runs inside pcall, so a failure says why in the Console instead of ending
-- the script without a word, which is what a menu script does on an error.
local function finish()
-- The Console gives `resolve` (or `Resolve()`); a script from the Scripts menu gets `bmd`.
local resolve = resolve
	or (type(Resolve) == "function" and Resolve())
	or (bmd and bmd.scriptapp("Resolve"))
	or (fusion and fusion.GetResolve and fusion:GetResolve())
if not resolve then
	say("no Resolve to talk to; run this from inside DaVinci Resolve")
	return
end

local manager = resolve:GetProjectManager()
-- The project already open is used as it is: loading the open project again fails, creating one
-- under a name taken fails too, and the second run of this script stopped there.
local open = manager:GetCurrentProject()
local project = (open and open:GetName() == NAME and open)
	or manager:LoadProject(NAME)
	or manager:CreateProject(NAME)
if not project then
	say("could not open or create the project " .. NAME)
	return
end
local pool = project:GetMediaPool()

-- A new timeline each run, named for when, so a second run never collides with the first.
local stamp = (os and os.date) and os.date("%Y-%m-%d %H.%M.%S") or tostring(#pool:GetRootFolder():GetClipList())
local timelineName = NAME .. " " .. stamp
local timeline = pool:ImportTimelineFromFile(FCPXML, { timelineName = timelineName, importSourceClips = true })
if not timeline then
	say("Resolve did not import " .. FCPXML)
	return
end
project:SetCurrentTimeline(timeline)

-- D290: every clip the timeline uses must have come in with its media.
local items, offline = {}, {}
for _, kind in ipairs({ "video", "audio" }) do
	for track = 1, timeline:GetTrackCount(kind) do
		for _, item in pairs(timeline:GetItemListInTrack(kind, track) or {}) do
			table.insert(items, item)
		end
	end
end
for _, item in ipairs(items) do
	local media = item:GetMediaPoolItem()
	local status = media and media:GetClipProperty("Online Status") or ""
	if type(status) == "table" then
		status = status["Online Status"] or ""
	end
	if not media or string.find(string.lower(tostring(status)), "offline", 1, true) then
		table.insert(offline, item:GetName())
	end
end
say(string.format(
	"%d clips on %d video and %d audio tracks, %d offline%s",
	#items,
	timeline:GetTrackCount("video"),
	timeline:GetTrackCount("audio"),
	#offline,
	(#offline > 0) and (": " .. table.concat(offline, ", ", 1, math.min(#offline, 10))) or ""
))

-- The narration's captions, the .srt written beside the MP4, on a subtitle track.
local captions = pool:ImportMedia({ SRT }) or {}
if captions[1] then
	if timeline:GetTrackCount("subtitle") < 1 then
		timeline:AddTrack("subtitle")
	end
	local placed = pool:AppendToTimeline({ { mediaPoolItem = captions[1], startFrame = 0, trackIndex = 1 } })
	say((placed and next(placed)) and "captions on the subtitle track"
		or "captions imported, not placed: drag them onto the timeline")
else
	say("could not import " .. SRT)
end

local saved = false
for _, preset in pairs(resolve:GetFairlightPresets() or {}) do
	if preset == FAIRLIGHT_PRESET then
		saved = true
	end
end
if saved then
	say(project:ApplyFairlightPresetToCurrentTimeline(FAIRLIGHT_PRESET) and "Fairlight preset applied"
		or "Fairlight preset not applied")
else
	say('no Fairlight preset "' .. FAIRLIGHT_PRESET .. '" saved yet: the dialogue chain is left as it is')
end

-- YouTube 1080p, the captions embedded, queued and not started.
project:SetCurrentRenderFormatAndCodec("mp4", "H264")
project:SetRenderSettings({
	TargetDir = RENDER_DIR,
	CustomName = NAME .. ".resolve",
	ExportVideo = true,
	ExportAudio = true,
	ExportSubtitle = true,
	SubtitleFormat = "EmbeddedCaptions",
})
local job = project:AddRenderJob()
say((job and job ~= "") and "render queued on the Deliver page" or "the render was not queued")
manager:SaveProject()
say("done: project " .. NAME .. " is open with the timeline")
end

say("starting, " .. NAME)
local ok, problem = pcall(finish)
if not ok then
	say("stopped: " .. tostring(problem))
end
if log then
	pcall(function()
		log:close()
	end)
end

-- What it did, in a window: a script run from the Scripts menu prints nowhere a person sees,
-- and Resolve may not let it write the log either.
pcall(function()
	local fusionApp = fu or fusion or (resolve and resolve:Fusion())
	local ui = fusionApp.UIManager
	local dispatcher = bmd.UIDispatcher(ui)
	local window = dispatcher:AddWindow({
		ID = "VigletFinish",
		WindowTitle = "Viglet finish: " .. NAME,
		Geometry = { 200, 200, 760, 320 },
	}, ui:VGroup({
		ui:TextEdit({ ID = "Report", ReadOnly = true, Text = table.concat(logged, "\n") }),
		ui:Button({ ID = "Close", Text = "Close", Weight = 0 }),
	}))
	function window.On.VigletFinish.Close()
		dispatcher:ExitLoop()
	end
	function window.On.Close.Clicked()
		dispatcher:ExitLoop()
	end
	window:Show()
	dispatcher:RunLoop()
	window:Hide()
end)
