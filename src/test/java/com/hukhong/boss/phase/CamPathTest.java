package com.hukhong.boss.phase;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CamPathTest {
    private static final List<CamPath.Key> KEYS = List.of(
            new CamPath.Key(0, 0, 1, 4, 0, 0, 70, 1),
            new CamPath.Key(40, 2, 2, 2, 10, -5, 50, 2),
            new CamPath.Key(100, -1, 3, 8, 0, 0, 80, 1));

    @Test void hits_keyframes_exactly() {
        for (CamPath.Key k : KEYS) {
            CamPath.Pose p = CamPath.sample(KEYS, k.t());
            assertEquals(k.dx(), p.dx(), 1e-4);
            assertEquals(k.dy(), p.dy(), 1e-4);
            assertEquals(k.dz(), p.dz(), 1e-4);
            assertEquals(k.fov(), p.fov(), 1e-4);
        }
    }

    @Test void clamps_outside_range() {
        assertEquals(KEYS.get(0).dx(), CamPath.sample(KEYS, -5).dx(), 1e-6);
        assertEquals(KEYS.get(2).fov(), CamPath.sample(KEYS, 500).fov(), 1e-6);
    }

    @Test void smooth_no_jumps_between_samples() {
        CamPath.Pose prev = CamPath.sample(KEYS, 0);
        for (int t = 1; t <= 100; t++) {
            CamPath.Pose p = CamPath.sample(KEYS, t);
            assertTrue(Math.abs(p.dx() - prev.dx()) < 0.5 && Math.abs(p.dz() - prev.dz()) < 0.5, "buoc nhay lon tai t=" + t);
            assertTrue(Math.abs(p.fov() - prev.fov()) < 3.0, "FOV nhay tai t=" + t);
            prev = p;
        }
    }

    @Test void single_and_empty() {
        assertEquals(70, CamPath.sample(List.of(KEYS.get(0)), 10).fov(), 1e-6);
        assertEquals(70, CamPath.sample(List.of(), 10).fov(), 1e-6);
    }

    @Test void envelope_fades_in_and_out() {
        assertEquals(0f, CamPath.envelope(0, 100, 10, 10), 1e-6);
        assertEquals(1f, CamPath.envelope(50, 100, 10, 10), 1e-6);
        assertEquals(0f, CamPath.envelope(100, 100, 10, 10), 1e-6);
        assertEquals(0.5f, CamPath.envelope(5, 100, 10, 10), 1e-6);
    }
}
