package com.hukhong.skill;

import com.hukhong.core.Path;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SkillPagesTest {
    @Test
    void orderIsFixed() {
        assertEquals(List.of(Path.KIEM, Path.THE, Path.MA), SkillPages.owned(Set.of(Path.MA, Path.THE, Path.KIEM)));
    }

    @Test
    void cyclesAndWraps() {
        assertEquals(1, SkillPages.next(0, 2));
        assertEquals(0, SkillPages.next(1, 2));
        assertEquals(0, SkillPages.next(2, 3));
        assertEquals(0, SkillPages.next(5, 0));
    }

    @Test
    void activeClampsStalePage() {
        Set<Path> two = Set.of(Path.DAO, Path.MA);
        assertEquals(Path.DAO, SkillPages.active(two, 0));
        assertEquals(Path.MA, SkillPages.active(two, 1));
        assertEquals(Path.DAO, SkillPages.active(two, 2));
        assertNull(SkillPages.active(Set.of(), 0));
    }
}
