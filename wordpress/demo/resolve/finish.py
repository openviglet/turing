# Finishing the demo video in DaVinci Resolve (D293): what FCPXML cannot carry, applied the same
# way every time. `node bin/story-video.mjs --language <lang> --project` writes this file into
# story.<lang>.project/ with its paths filled in; nothing here needs editing.
#
# Resolve's free version runs scripts only from inside the app, so run it there:
#   Workspace > Console > Py3, then:  exec(open(r"<the project folder>\finish.py").read())
# or copy it into %APPDATA%\Blackmagic Design\DaVinci Resolve\Support\Fusion\Scripts\Utility\
# and pick it under Workspace > Scripts.
#
# It creates (or opens) a project named for the video, imports the timeline, says whether any
# clip came in offline (D290's check), adds the narration's captions as a subtitle track, applies
# the Fairlight preset named below when one is saved, and queues a YouTube render. It starts no
# render and changes nothing outside that project.

NAME = "__NAME__"
FCPXML = r"__FCPXML__"
SRT = r"__SRT__"
RENDER_DIR = r"__RENDER_DIR__"
# Save the dialogue chain (EQ, de-esser, Dialogue Leveler) once as a Fairlight preset with this
# name, and every run applies it.
FAIRLIGHT_PRESET = "Viglet story voice"


def say(text):
    print("finish: " + text)


try:
    resolve  # noqa: F821 - given to a script run inside Resolve
except NameError:
    import DaVinciResolveScript as dvr

    resolve = dvr.scriptapp("Resolve")

manager = resolve.GetProjectManager()
project = manager.LoadProject(NAME) or manager.CreateProject(NAME)
if not project:
    raise SystemExit("finish: could not open or create the project " + NAME)
pool = project.GetMediaPool()

timeline = pool.ImportTimelineFromFile(
    FCPXML, {"timelineName": NAME, "importSourceClips": True}
)
if not timeline:
    raise SystemExit("finish: Resolve did not import " + FCPXML)
project.SetCurrentTimeline(timeline)

# D290: every clip the timeline uses must have come in with its media.
items = []
for kind in ("video", "audio"):
    for track in range(1, timeline.GetTrackCount(kind) + 1):
        items += timeline.GetItemListInTrack(kind, track) or []
offline = []
for item in items:
    media = item.GetMediaPoolItem()
    properties = media.GetClipProperty() if media else {}
    if not media or "offline" in str(properties.get("Online Status", "")).lower():
        offline.append(item.GetName())
say(
    "{} clips on {} video and {} audio tracks, {} offline{}".format(
        len(items),
        timeline.GetTrackCount("video"),
        timeline.GetTrackCount("audio"),
        len(offline),
        (": " + ", ".join(offline[:10])) if offline else "",
    )
)

# The narration's captions, the .srt written beside the MP4, on a subtitle track.
captions = pool.ImportMedia([SRT]) or []
if captions:
    if timeline.GetTrackCount("subtitle") < 1:
        timeline.AddTrack("subtitle")
    placed = pool.AppendToTimeline(
        [{"mediaPoolItem": captions[0], "startFrame": 0, "trackIndex": 1}]
    )
    say("captions on the subtitle track" if placed else "captions imported, not placed: drag them onto the timeline")
else:
    say("could not import " + SRT)

if FAIRLIGHT_PRESET in (resolve.GetFairlightPresets() or []):
    applied = project.ApplyFairlightPresetToCurrentTimeline(FAIRLIGHT_PRESET)
    say("Fairlight preset applied" if applied else "Fairlight preset not applied")
else:
    say('no Fairlight preset "' + FAIRLIGHT_PRESET + '" saved yet: the dialogue chain is left as it is')

# YouTube 1080p, the captions embedded, queued and not started.
project.SetCurrentRenderFormatAndCodec("mp4", "H264")
project.SetRenderSettings(
    {
        "TargetDir": RENDER_DIR,
        "CustomName": NAME + ".resolve",
        "ExportVideo": True,
        "ExportAudio": True,
        "ExportSubtitle": True,
        "SubtitleFormat": "EmbeddedCaptions",
    }
)
job = project.AddRenderJob()
say("render queued on the Deliver page" if job else "the render was not queued")
manager.SaveProject()
