package org.orvencraft.uicore.api.cutscene;

import java.util.UUID;

/** Immutable state. Time is measured in server ticks, excluding pauses and camera preparation. */
public record CutsceneSnapshot(UUID sessionId, String id, CutsceneState state, String stepId,
                               long elapsedTicks, long stepElapsedTicks, double stepProgress) { }
