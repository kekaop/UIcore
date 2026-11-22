package org.orvencraft.uicore.api.cutscene;

/** ACCEPTED confirms the request; CutsceneStartEvent confirms camera preparation and actual playback. */
public enum CutsceneStartResult {
   ACCEPTED, UNKNOWN_SCENE, NO_MENU, ALREADY_RUNNING
}
