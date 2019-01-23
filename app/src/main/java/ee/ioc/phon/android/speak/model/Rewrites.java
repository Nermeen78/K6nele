package ee.ioc.phon.android.speak.model;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.speech.RecognizerIntent;
import android.util.Base64;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.activity.RewritesActivity;
import ee.ioc.phon.android.speechutils.Extras;
import ee.ioc.phon.android.speechutils.editor.Command;
import ee.ioc.phon.android.speechutils.editor.UtteranceRewriter;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;

public class Rewrites {

    private static final Comparator SORT_BY_ID = new Rewrites.SortById();

    private SharedPreferences mPrefs;
    private Resources mRes;

    private final String mId;

    public Rewrites(SharedPreferences prefs, Resources res, String id) {
        mPrefs = prefs;
        mRes = res;
        mId = id;
    }

    public String getId() {
        return mId;
    }

    public String toString() {
        return mId;
    }

    public boolean isSelected() {
        return getDefaults().contains(mId);
    }

    public void setSelected(boolean b) {
        Set<String> set = new HashSet<>(getDefaults());
        if (set.contains(mId)) {
            if (!b) {
                set.remove(mId);
                putDefaults(set);
            }
        } else {
            if (b) {
                set.add(mId);
                putDefaults(set);
            }
        }
    }

    public Intent getK6neleIntent() {
        Intent intent = new Intent();
        intent.setClassName("ee.ioc.phon.android.speak", "ee.ioc.phon.android.speak.activity.SpeechActionActivity");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, mId);
        intent.putExtra(Extras.EXTRA_AUTO_START, true);
        intent.putExtra(Extras.EXTRA_RESULT_REWRITES, new String[]{mId});
        return intent;
    }

    public Intent getShowIntent() {
        Intent intent = new Intent();
        intent.setClassName("ee.ioc.phon.android.speak", "ee.ioc.phon.android.speak.activity.RewritesActivity");
        intent.putExtra(RewritesActivity.EXTRA_NAME, mId);
        return intent;
    }

    public Intent getSendIntent() {
        String rewrites = PreferenceUtils.getPrefMapEntry(mPrefs, mRes, R.string.keyRewritesMap, mId);
        UtteranceRewriter ur = new UtteranceRewriter(rewrites);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT, mId);
        intent.putExtra(Intent.EXTRA_TEXT, ur.toTsv());
        intent.setType("text/tab-separated-values");
        return intent;
    }

    public Intent getIntentSendBase64() {
        String rewrites = PreferenceUtils.getPrefMapEntry(mPrefs, mRes, R.string.keyRewritesMap, mId);
        UtteranceRewriter ur = new UtteranceRewriter(rewrites);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT, mId);
        intent.putExtra(Intent.EXTRA_TEXT, "k6://" + Base64.encodeToString(ur.toTsv().getBytes(), Base64.NO_WRAP | Base64.URL_SAFE));
        intent.setType("text/plain");
        return intent;
    }

    public String[] getRules() {
        String rewrites = PreferenceUtils.getPrefMapEntry(mPrefs, mRes, R.string.keyRewritesMap, mId);
        UtteranceRewriter ur = new UtteranceRewriter(rewrites);

        UtteranceRewriter.CommandHolder holder = ur.getCommandHolder();
        Collection<String> header = holder.getHeader().values();
        String[] array = new String[holder.size()];
        int i = 0;
        for (Command command : holder.getCommands()) {
            array[i++] = pp(command.toMap(header));
        }
        return array;
    }

    public void rename(String newName) {
        if (!mId.equals(newName)) {
            if (newName != null) {
                String rewrites = PreferenceUtils.getPrefMapEntry(mPrefs, mRes, R.string.keyRewritesMap, mId);
                PreferenceUtils.putPrefMapEntry(mPrefs, mRes, R.string.keyRewritesMap, newName, rewrites);
            }
            Set<String> deleteKeys = new HashSet<>();
            deleteKeys.add(mId);
            PreferenceUtils.clearPrefMap(mPrefs, mRes, R.string.keyRewritesMap, deleteKeys);
            Set<String> defaults = new HashSet<>(getDefaults());
            if (defaults.contains(mId)) {
                defaults.remove(mId);
                if (newName != null) {
                    defaults.add(newName);
                }
                putDefaults(defaults);
            }
        }
    }

    public void delete() {
        rename(null);
    }

    private Set<String> getDefaults() {
        return PreferenceUtils.getPrefStringSet(mPrefs, mRes, R.string.defaultRewriteTables);
    }

    private void putDefaults(Set<String> set) {
        PreferenceUtils.putPrefStringSet(mPrefs, mRes, R.string.defaultRewriteTables, set);
    }

    public static Set<String> getDefaults(SharedPreferences prefs, Resources res) {
        return PreferenceUtils.getPrefStringSet(prefs, res, R.string.defaultRewriteTables);
    }

    public static List<Rewrites> getTables(SharedPreferences prefs, Resources res) {
        List<String> rewritesIds = new ArrayList<>(PreferenceUtils.getPrefMapKeys(prefs, res, R.string.keyRewritesMap));
        List<Rewrites> rewritesTables = new ArrayList<>();
        for (String id : rewritesIds) {
            rewritesTables.add(new Rewrites(prefs, res, id));
        }
        Collections.sort(rewritesTables, SORT_BY_ID);
        return rewritesTables;
    }

    private static class SortById implements Comparator {

        public int compare(Object o1, Object o2) {
            Rewrites c1 = (Rewrites) o1;
            Rewrites c2 = (Rewrites) o2;
            return c1.getId().compareToIgnoreCase(c2.getId());
        }
    }

    /**
     * Pretty-print the rule, assuming that space-character sequences actually stand
     * for newline (single space) followed by tab (two spaces) sequences.
     * Sequence of 1 or 2 spaces is kept as it is.
     * <p>
     * TODO: map it to a layout file
     * TODO: handle null values
     *
     * @param map Mapping of rule component names to the corresponding components
     * @return pretty-printed rule
     */
    private static String pp(Map<String, String> map) {
        return
                map.get(UtteranceRewriter.HEADER_APP) + " · " +
                        map.get(UtteranceRewriter.HEADER_LOCALE) + " · " +
                        map.get(UtteranceRewriter.HEADER_SERVICE) + "\n" +
                        map.get(UtteranceRewriter.HEADER_UTTERANCE) + "\n" +
                        map.get(UtteranceRewriter.HEADER_REPLACEMENT)
                                .replace("         ", "\n\t\t\t\t")
                                .replace("       ", "\n\t\t\t")
                                .replace("     ", "\n\t\t")
                                .replace("   ", "\n\t") +
                        ppCommand(map) +
                        ppComment(map);
    }

    private static String ppCommand(Map<String, String> map) {
        String id = map.get(UtteranceRewriter.HEADER_COMMAND);
        if (id == null || id.isEmpty()) {
            return "";
        }
        String arg1 = map.get(UtteranceRewriter.HEADER_ARG1);
        String arg2 = map.get(UtteranceRewriter.HEADER_ARG2);
        return "\n" + id + "\n· " + arg1 + "\n· " + arg2 + "\n";
    }

    private static String ppComment(Map<String, String> map) {
        String id = map.get(UtteranceRewriter.HEADER_COMMENT);
        if (id == null || id.isEmpty()) {
            return "";
        }
        return "\n" + id;
    }
}