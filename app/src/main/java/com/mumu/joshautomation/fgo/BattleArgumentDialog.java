package com.mumu.joshautomation.fgo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mumu.joshautomation.R;
import com.mumu.libjoshgame.Log;

import java.util.ArrayList;

public class BattleArgumentDialog extends Activity {
    private static final String TAG = "BattleArgumentDialog";
    public static String bundlePreferenceKey = "preferenceKey";
    private UISet mUISet;
    private ArrayList<StringBuilder> mArgs;
    private int mCurrentArg;
    private boolean mChangingServant = false;
    private String mCurrentPreferenceKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Bundle bundle = getIntent().getExtras();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.battle_argument_dialog);
        Log.d(TAG, "Get preference key: " + bundle.getString(bundlePreferenceKey));
        mCurrentPreferenceKey = bundle.getString(bundlePreferenceKey);

        mUISet = new UISet(this);

        mArgs = new ArrayList<>();
        for(int i = 0; i < 3; i++) {
            mArgs.add(new StringBuilder(""));
        }
        mCurrentArg = 0;

        // confirm button
        findViewById(R.id.buttonBAConfirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuilder builder = new StringBuilder("");
                for(StringBuilder b: mArgs) {
                    builder.append(b.toString());
                    builder.append("|");
                }
                Log.d(TAG, "Final arg = " + builder.toString());
                updateArgToPreference(builder.toString());
                finish();
            }
        });

        // cancel button
        findViewById(R.id.buttonBACancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void updateArgToPreference(String value) {
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(this).edit();
        prefs.putString(mCurrentPreferenceKey, value);
        prefs.apply();
    }

    private void updateArgTextView() {
        for(int i = 0; i < 3; i++) {
            String argDisplayed = "關卡" + (i+1) + ": " + mArgs.get(i).toString();
            mUISet.stageArgumentText.get(i).setText(argDisplayed);
        }
    }

    private void checkBackspace() {
        StringBuilder builder = mArgs.get(mCurrentArg);
        String currentArg = builder.toString();

        if (currentArg.length() > 0) {
            mUISet.changeState(UISet.STATE_ENABLE_BS);
        } else {
            mUISet.changeState(UISet.STATE_DISABLE_BS);
        }
    }

    private void appendArg(String value) {
        StringBuilder builder = mArgs.get(mCurrentArg);

        builder.append(value);
        updateArgTextView();
        checkBackspace();
    }

    private void deleteArg() {
        StringBuilder builder = mArgs.get(mCurrentArg);
        String currentArg = builder.toString();

        if (currentArg.length() == 0) {
            Log.d(TAG, "arg " + mCurrentArg + " has no string");
            return;
        }

        builder.deleteCharAt(currentArg.length() - 1);
        updateArgTextView();
        checkBackspace();
    }

    private void onRoyalSelected(int royal) {
        String value;

        switch (royal) {
            case 0:
                value = "6";
                break;
            case 1:
                value = "7";
                break;
            case 2:
                value = "8";
                break;
            default:
                Log.e(TAG, "WTF: royal tap value illegal " + royal);
                return;
        }

        //mUISet.changeState(UISet.STATE_TARGET_03); // Current Royal not support target
        appendArg(value);
    }

    private void onSkillSelected(int skill) {
        String value;

        switch (skill) {
            case 0:
                value = "a";
                break;
            case 1:
                value = "b";
                break;
            case 2:
                value = "c";
                break;
            case 3:
                value = "e";
                break;
            case 4:
                value = "f";
                break;
            case 5:
                value = "g";
                break;
            case 6:
                value = "i";
                break;
            case 7:
                value = "j";
                break;
            case 8:
                value = "k";
                break;
            default:
                Log.e(TAG, "WTF: skill tap value illegal " + skill);
                return;
        }
        mUISet.changeState(UISet.STATE_TARGET_03);
        appendArg(value);
    }

    private void onMasterSelected(int master) {
        String value;

        switch (master) {
            case 0:
                value = "x";
                break;
            case 1:
                value = "y";
                break;
            case 2:
                value = "z";
                break;
            default:
                Log.e(TAG, "WTF: master tap value illegal " + master);
                return;
        }
        mUISet.changeState(UISet.STATE_TARGET_03);
        appendArg(value);
    }

    private void onChangeServant() {
        mUISet.changeState(UISet.STATE_TARGET_13);
        appendArg("w");
        mChangingServant = true;
    }

    private void onTargetSelected(int target) {
        String value;

        switch (target) {
            case 0:
                value = "0";
                break;
            case 1:
                value = "1";
                break;
            case 2:
                value = "2";
                break;
            case 3:
                value = "3";
                break;
            case 4:
                value = "1";
                break;
            case 5:
                value = "2";
                break;
            case 6:
                value = "3";
                break;
            default:
                Log.e(TAG, "WTF: target tap value illegal " + target);
                return;
        }

        if (mChangingServant) {
            mUISet.changeState(UISet.STATE_TARGET_46);
            mChangingServant = false;
        } else {
            mUISet.changeState(UISet.STATE_SKILL_ALL);
        }
        appendArg(value);
    }

    private void onStageChanged(int currentStage) {
        Log.d(TAG, "onStageChanged: index = " + currentStage);
        mUISet.changeState(currentStage + 4);
        mCurrentArg = currentStage;
        checkBackspace();
    }

    private void onRoundTapped() {
        Log.d(TAG, "onRoundTapped");
        appendArg("#");
    }

    private void onBackspaceTapped() {
        Log.d(TAG, "onBackspaceTapped");
        mUISet.changeState(UISet.STATE_SKILL_ALL);
        deleteArg();
    }

    private class UISet {
        private Activity mActivity;

        static final int STATE_SKILL_ALL  = 0;
        static final int STATE_TARGET_03  = 1;
        static final int STATE_TARGET_13  = 2;
        static final int STATE_TARGET_46  = 3;
        static final int STATE_STAGE_1    = 4;
        static final int STATE_STAGE_2    = 5;
        static final int STATE_STAGE_3    = 6;
        static final int STATE_ENABLE_BS  = 7;
        static final int STATE_DISABLE_BS = 8;

        ArrayList<Button> royalButtons = new ArrayList<>();
        ArrayList<Button> skillButtons = new ArrayList<>();
        ArrayList<Button> masterButtons = new ArrayList<>();
        ArrayList<Button> targetButtons = new ArrayList<>();
        ArrayList<Button> stageButtons = new ArrayList<>();
        Button changeServantButton;
        Button roundButton;
        Button backspaceButton;
        TextView instructionText;
        ArrayList<TextView> stageArgumentText = new ArrayList<>();

        UISet(Activity act) {
            mActivity = act;
            init();
        }

        private void init() {
            royalButtons.add(0, getAndInitButton(R.id.buttonRoyal1));
            royalButtons.add(1, getAndInitButton(R.id.buttonRoyal2));
            royalButtons.add(2, getAndInitButton(R.id.buttonRoyal3));

            skillButtons.add(0, getAndInitButton(R.id.buttonSkill1));
            skillButtons.add(1, getAndInitButton(R.id.buttonSkill2));
            skillButtons.add(2, getAndInitButton(R.id.buttonSkill3));
            skillButtons.add(3, getAndInitButton(R.id.buttonSkill4));
            skillButtons.add(4, getAndInitButton(R.id.buttonSkill5));
            skillButtons.add(5, getAndInitButton(R.id.buttonSkill6));
            skillButtons.add(6, getAndInitButton(R.id.buttonSkill7));
            skillButtons.add(7, getAndInitButton(R.id.buttonSkill8));
            skillButtons.add(8, getAndInitButton(R.id.buttonSkill9));

            targetButtons.add(0, getAndInitButton(R.id.buttonTarget0));
            targetButtons.add(1, getAndInitButton(R.id.buttonTarget1));
            targetButtons.add(2, getAndInitButton(R.id.buttonTarget2));
            targetButtons.add(3, getAndInitButton(R.id.buttonTarget3));
            targetButtons.add(4, getAndInitButton(R.id.buttonTarget4));
            targetButtons.add(5, getAndInitButton(R.id.buttonTarget5));
            targetButtons.add(6, getAndInitButton(R.id.buttonTarget6));

            masterButtons.add(0, getAndInitButton(R.id.buttonMaster1));
            masterButtons.add(1, getAndInitButton(R.id.buttonMaster2));
            masterButtons.add(2, getAndInitButton(R.id.buttonMaster3));
            changeServantButton = getAndInitButton(R.id.buttonMaster4);

            stageButtons.add(0, getAndInitButton(R.id.buttonStage1));
            stageButtons.add(1, getAndInitButton(R.id.buttonStage2));
            stageButtons.add(2, getAndInitButton(R.id.buttonStage3));
            
            roundButton = getAndInitButton(R.id.buttonRound);
            backspaceButton = getAndInitButton(R.id.buttonBackspace);

            instructionText = getAndInitTextView(R.id.textViewInstruction);
            stageArgumentText.add(0, getAndInitTextView(R.id.textViewBattleArgumentStage1));
            stageArgumentText.add(1, getAndInitTextView(R.id.textViewBattleArgumentStage2));
            stageArgumentText.add(2, getAndInitTextView(R.id.textViewBattleArgumentStage3));

            changeState(STATE_SKILL_ALL);
            changeState(STATE_STAGE_1);
            changeState(STATE_DISABLE_BS);
        }

        void changeState(int state) {
            ArrayList<Button> target03 = new ArrayList<>();
            ArrayList<Button> target13 = new ArrayList<>();
            ArrayList<Button> target46 = new ArrayList<>();
            for(int i = 0; i <= 3; i++)
                target03.add(targetButtons.get(i));

            for(int i = 1; i <= 3; i++)
                target13.add(targetButtons.get(i));

            for(int i = 4; i <= 6; i++)
                target46.add(targetButtons.get(i));

            switch (state) {
                case STATE_SKILL_ALL:
                    triggerEnableButtonSet(targetButtons, false);
                    triggerEnableButtonSet(royalButtons, true);
                    triggerEnableButtonSet(skillButtons, true);
                    triggerEnableButtonSet(masterButtons, true);
                    break;
                case STATE_TARGET_03:
                    triggerEnableButtonSet(target03, true);
                    triggerEnableButtonSet(target46, false);
                    triggerEnableButtonSet(royalButtons, true);
                    triggerEnableButtonSet(skillButtons, true);
                    triggerEnableButtonSet(masterButtons, true);
                    break;
                case STATE_TARGET_13:
                    triggerEnableButtonSet(target13, true);
                    triggerEnableButtonSet(target46, false);
                    triggerEnableButtonSet(royalButtons, false);
                    triggerEnableButtonSet(skillButtons, false);
                    triggerEnableButtonSet(masterButtons, false);
                    break;
                case STATE_TARGET_46:
                    triggerEnableButtonSet(target46, true);
                    triggerEnableButtonSet(target13, false);
                    triggerEnableButtonSet(royalButtons, false);
                    triggerEnableButtonSet(skillButtons, false);
                    triggerEnableButtonSet(masterButtons, false);
                    break;
                case STATE_STAGE_1:
                    stageButtons.get(0).setEnabled(false);
                    stageButtons.get(1).setEnabled(true);
                    stageButtons.get(2).setEnabled(true);
                    stageArgumentText.get(0).setTextColor(Color.BLACK);
                    stageArgumentText.get(1).setTextColor(Color.GRAY);
                    stageArgumentText.get(2).setTextColor(Color.GRAY);
                    break;
                case STATE_STAGE_2:
                    stageButtons.get(0).setEnabled(true);
                    stageButtons.get(1).setEnabled(false);
                    stageButtons.get(2).setEnabled(true);
                    stageArgumentText.get(0).setTextColor(Color.GRAY);
                    stageArgumentText.get(1).setTextColor(Color.BLACK);
                    stageArgumentText.get(2).setTextColor(Color.GRAY);
                    break;
                case STATE_STAGE_3:
                    stageButtons.get(0).setEnabled(true);
                    stageButtons.get(1).setEnabled(true);
                    stageButtons.get(2).setEnabled(false);
                    stageArgumentText.get(0).setTextColor(Color.GRAY);
                    stageArgumentText.get(1).setTextColor(Color.GRAY);
                    stageArgumentText.get(2).setTextColor(Color.BLACK);
                    break;
                case STATE_ENABLE_BS:
                    backspaceButton.setEnabled(true);
                    break;
                case STATE_DISABLE_BS:
                    backspaceButton.setEnabled(false);
                    break;
                default:
                    Log.w(TAG, "Not defined state " + state);
            }
        }

        private void triggerEnableButtonSet(ArrayList<Button> buttons, boolean enable) {
            for(Button button: buttons) {
                button.setEnabled(enable);
            }
        }

        private View.OnClickListener mButtonClickCallbacks = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();

                if (id >= R.id.buttonRoyal1 && id <= R.id.buttonRoyal3) {
                    onRoyalSelected(id - R.id.buttonRoyal1);
                } else if (id >= R.id.buttonSkill1 && id <= R.id.buttonSkill9) {
                    onSkillSelected(id - R.id.buttonSkill1);
                } else if (id >= R.id.buttonTarget0 && id <= R.id.buttonTarget6) {
                    onTargetSelected(id - R.id.buttonTarget0);
                } else if (id >= R.id.buttonMaster1 && id <= R.id.buttonMaster3) {
                    onMasterSelected(id - R.id.buttonMaster1);
                } else if (id == R.id.buttonMaster4) {
                    onChangeServant();
                } else if (id >= R.id.buttonStage1 && id <= R.id.buttonStage3) {
                    onStageChanged(id - R.id.buttonStage1);
                } else if (id == R.id.buttonRound) {
                    onRoundTapped();
                } else if (id == R.id.buttonBackspace) {
                    onBackspaceTapped();
                } else {
                    Log.e(TAG, "Not handled button press, id = " + id);
                }
            }
        };

        private Button getAndInitButton(int id) {
            Button button = mActivity.findViewById(id);
            button.setOnClickListener(mButtonClickCallbacks);
            return button;
        }

        private TextView getAndInitTextView(int id) {
            return mActivity.findViewById(id);
        }
    }
}
