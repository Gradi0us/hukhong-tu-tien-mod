package com.hukhong.stats;

import com.hukhong.core.Aptitude;
import com.hukhong.core.Path;
import com.hukhong.cores.CoreService;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StatServiceTest {
    @Test
    void kiemLevel100() {
        var s = StatService.compute(100, Set.of(Path.KIEM), Map.of(Path.KIEM, Aptitude.B));
        assertEquals(162.4, s.attack(), 0.01);
        assertEquals(595, s.maxResource(), 0.01);
        assertEquals(0.347, s.crit(), 0.001);
    }

    @Test
    void milestones() {
        assertEquals(1, CoreService.milestoneOfLevel(10));
        assertEquals(9, CoreService.milestoneOfLevel(90));
        assertEquals(10, CoreService.milestoneOfLevel(95));
        assertEquals(11, CoreService.milestoneOfLevel(99));
    }

    @Test
    void defenseDiminishing() {
        assertEquals(50f, StatService.mitigate(100f, 100), 0.01);
    }
}
