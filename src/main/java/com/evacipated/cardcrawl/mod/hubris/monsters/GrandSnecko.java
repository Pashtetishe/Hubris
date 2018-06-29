package com.evacipated.cardcrawl.mod.hubris.monsters;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.evacipated.cardcrawl.mod.hubris.actions.monsterOrbs.MonsterChannelAction;
import com.evacipated.cardcrawl.mod.hubris.actions.monsterOrbs.MonsterIncreaseMaxOrbAction;
import com.evacipated.cardcrawl.mod.hubris.orbs.monster.*;
import com.evacipated.cardcrawl.mod.hubris.powers.UnfocusedPower;
import com.megacrit.cardcrawl.actions.animations.TalkAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.FrailPower;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import com.megacrit.cardcrawl.powers.WeakPower;
import com.megacrit.cardcrawl.vfx.SpeechBubble;
import javafx.util.Pair;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class GrandSnecko extends OrbUsingMonster
{
    public static final String ID = "hubris:OrbBoss";
    public static final String NAME = "Grand Snecko";
    public static final String[] MOVES = {};
    public static final String[] DIALOG = {
            "@THERE.@ @ARE.@ @FOUR.@ @LIGHTS.@",
            "~Tell~ ~me...~ NL How many lights do you see?"
    };
    public static final int HP = 200;

    private static final int ORB_SLOTS = 4;
    private static final ArrayList<Pair<Integer, Class>> orbPercents;
    private static final int maxPercent;

    static {
        orbPercents = new ArrayList<>();
        orbPercents.add(new Pair<>(40, MonsterLightning.class));
        orbPercents.add(new Pair<>(15, MonsterFrost.class));
        orbPercents.add(new Pair<>(10, MonsterFocusing.class));
        orbPercents.add(new Pair<>(5, MonsterMiasma.class));

        int sum = 0;
        for (Pair<Integer, Class> kv : orbPercents) {
            sum += kv.getKey();
        }
        maxPercent = sum;
    }

    public GrandSnecko()
    {
        super(NAME, ID, HP, -30.0f, -20.0f, 434, 427, null, -50.0f, 30.0f);
        maxOrbsCap = 12;
        loadAnimation("images/monsters/theCity/reptile/skeleton.atlas", "images/monsters/theCity/reptile/skeleton.json", 0.6F);

        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        this.stateData.setMix("Hit", "Idle", 0.1F);
        e.setTimeScale(0.8F);

        this.type = AbstractMonster.EnemyType.BOSS;
        this.dialogX = (-200.0F * Settings.scale);
        this.dialogY = (10.0F * Settings.scale);
    }

    private AbstractOrb makeOrb(Class clazz)
    {
        Constructor ctor = null;
        try {
            ctor = clazz.getConstructor(OrbUsingMonster.class);
            return (AbstractOrb)ctor.newInstance(this);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private AbstractOrb getRandomOrb()
    {
        int r = AbstractDungeon.aiRng.random(maxPercent);
        int sum = 0;
        for (Pair<Integer, Class> kv : orbPercents) {
            sum += kv.getKey();
            if (r <= sum) {
                return makeOrb(kv.getValue());
            }
        }
        return null;
    }

    private void firstChannel()
    {
        AbstractDungeon.actionManager.addToBottom(new TalkAction(this, DIALOG[1], 1.0f, 3.0f));

        AbstractDungeon.actionManager.addToBottom(new MonsterIncreaseMaxOrbAction(this, ORB_SLOTS));
        AbstractDungeon.actionManager.addToBottom(new MonsterChannelAction(this, new MonsterHypnotic(this)));
        for (int i=0; i<ORB_SLOTS-1; ++i) {
            AbstractDungeon.actionManager.addToBottom(new MonsterChannelAction(this, new MonsterLightning(this)));
        }
    }

    @Override
    public void usePreBattleAction()
    {
        CardCrawlGame.music.unsilenceBGM();
        AbstractDungeon.scene.fadeOutAmbiance();
        AbstractDungeon.getCurrRoom().playBgmInstantly("BOSS_BEYOND");
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new UnfocusedPower(this)));
        firstChannel();
    }

    private boolean rerollOrb(AbstractOrb orb)
    {
        if (orb == null) {
            return true;
        }

        // Check player's weak/vulnerable/frail
        int miasmaCount = 0;
        if (orb.ID.equals(MonsterMiasma.ORB_ID)) {
            for (AbstractPower power : AbstractDungeon.player.powers) {
                if (power.ID.equals(WeakPower.POWER_ID)
                        || power.ID.equals(VulnerablePower.POWER_ID)
                        || power.ID.equals(FrailPower.POWER_ID)) {
                    ++miasmaCount;
                }
            }
        }

        if (miasmaCount >= 3) {
            return true;
        }

        return false;
    }

    @Override
    public void takeTurn()
    {
        for (int i=0; i<numberToChannel; ++i) {
            AbstractOrb orb;
            do {
                orb = getRandomOrb();
            } while (rerollOrb(orb));
            AbstractDungeon.actionManager.addToBottom(new MonsterChannelAction(this, orb));
        }

        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    @Override
    protected void getMove(int i)
    {
        if (i == 0) {
            getMove(AbstractDungeon.aiRng.random(1, 99));
            return;
        } else if (i <= 33) {
            numberToChannel = 2;
        } else if (i <= 66) {
            numberToChannel = 3;
        } else if (i <= 99) {
            numberToChannel = 4;
        }

        setMove((byte)0, OrbUsingMonster.Enums.CHANNEL_ORBS, numberToChannel);
    }

    @Override
    public void die()
    {
        if (!AbstractDungeon.getCurrRoom().cannotLose) {
            AbstractPlayer p = AbstractDungeon.player;
            AbstractDungeon.effectList.add(new SpeechBubble(p.dialogX, p.dialogY, 3.0f, DIALOG[0], p.isPlayer));

            useFastShakeAnimation(5.0F);
            CardCrawlGame.screenShake.rumble(4.0F);
            this.deathTimer += 1.5F;
            super.die();
            onBossVictoryLogic();
            onFinalBossVictoryLogic();
        }
    }
}