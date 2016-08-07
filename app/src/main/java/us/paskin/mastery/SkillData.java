package us.paskin.mastery;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import us.paskin.mastery.Proto.Skill;

/**
 *
 */
public class SkillData {

    private static SkillData instance = null;

    protected List<Skill> skills;

    SkillData() {
        skills = new LinkedList<Skill>();
        addFakeData();
    }

    public static SkillData getInstance() {
        if (instance == null) instance = new SkillData();
        return instance;
    }

    public int numSkills() {
        return skills.size();
    }

    public Skill getSkillByIndex(int index) {
        return skills.get(index);
    }

    // Sets the id of the skill and stores it.  The index of the skill is returned.
    public int addSkill(Skill skill) {
        final int index = numSkills();
        skills.add(skill);
        return index;
    }

    public String timeSinceLastPracticedText(Skill skill) {
        // TODO: i18n
        if (!skill.hasDateLastPracticed()) {
            return "never";
        }
        final long millis = new Date().getTime() - new Date(skill.getDateLastPracticed()).getTime();
        final long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        if (diffInMinutes == 0) return "just now";
        final long diffInHours = TimeUnit.MILLISECONDS.toHours(millis);
        if (diffInHours == 0) return diffInMinutes + " minutes ago";
        final long diffInDays = TimeUnit.MILLISECONDS.toDays(millis);
        if (diffInDays == 0) return diffInHours + " hours ago";
        final long diffInWeeks = diffInDays / 7;
        if (diffInWeeks < 14) return diffInDays + " days ago";
        final long diffInMonths = diffInDays / 30;
        if (diffInMonths < 3) return diffInWeeks + " weeks ago";
        return diffInMonths + "months ago";
    }

    public void addFakeData() {
        addSkill(Skill.newBuilder()
                .setName("Carcassi Op. 60 No. 7")
                .setDateLastPracticed(new Date().getTime() - TimeUnit.DAYS.toMillis(2))
                .build());
        addSkill(Skill.newBuilder()
                .setName("Shearer Scale p. 253")
                .setDateLastPracticed(new Date().getTime() - TimeUnit.HOURS.toMillis(7))
                .build());
    }
}
