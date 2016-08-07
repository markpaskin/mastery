package us.paskin.mastery;

import java.util.List;

import us.paskin.mastery.Proto.Skill;

/**
 *
 */
public class SkillData {
    protected List<Skill> skills;

    SkillData() {
    }

    public int addSkill(Skill skill) {
        skills.add(skill);
        return 1;
    }
}
