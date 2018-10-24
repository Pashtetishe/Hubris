package com.evacipated.cardcrawl.mod.hubris.monsters;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.evacipated.cardcrawl.mod.hubris.HubrisMod;
import com.evacipated.cardcrawl.mod.hubris.actions.unique.SacrificeMinionAction;
import com.evacipated.cardcrawl.mod.hubris.powers.CursedLifePower;
import com.evacipated.cardcrawl.mod.hubris.powers.FakeDeathPower;
import com.evacipated.cardcrawl.mod.hubris.vfx.scene.NecromanticTotemParticleEffect;
import com.evacipated.cardcrawl.modthespire.lib.SpireOverride;
import com.evacipated.cardcrawl.modthespire.lib.SpireSuper;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.status.Void;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.*;
import com.megacrit.cardcrawl.vfx.CollectorCurseEffect;

public class NecromanticTotem extends AbstractMonster
{
    public static final String ID = "hubris:NecromanticTotem";
    public static final String NAME = "Necromantic Totem";
    public static final String[] MOVES = {};
    public static final int HP = 50;
    private static final int CURSE_AMT = 7;
    private static final int STRENGTH_AMT = 3;
    private static final int INTANGIBLE_AMT = 1;
    private static final int MEGA_DEBUFF_AMT = 3;

    private static final byte SUMMON = 0;
    private static final byte BUFF1 = 1;
    private static final byte BUFF2 = 2;
    private static final byte DEBUFF1 = 3;
    private static final byte DEBUFF2 = 4;

    private static final float PARTICAL_EMIT_INTERVAL = 0.15f;

    private static ShaderProgram shader = new ShaderProgram(
            Gdx.files.internal(HubrisMod.assetPath("shaders/totem/vertexShader.vs")),
            Gdx.files.internal(HubrisMod.assetPath("shaders/totem/fragShader.fs"))
    );

    private float particleTimer = 0.0f;

    private int numTurns = 0;

    public NecromanticTotem()
    {
        super(NAME, ID, HP, -8.0f, 10.0f, 230, 300, HubrisMod.assetPath("images/monsters/theCity/necromanticTotem.png"), 100.0f, -30.0f);
        /*
        loadAnimation("images/monsters/theBottom/cultist/skeleton.atlas", "images/monsters/theBottom/cultist/skeleton.json", 0.75F);

        AnimationState.TrackEntry e = this.state.setAnimation(0, "waving", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        */

        this.type = AbstractMonster.EnemyType.BOSS;
        this.dialogX = (-400.0F * Settings.scale);
        this.dialogY = (200.0F * Settings.scale);

        damage.add(0, new DamageInfo(this, 50));
        damage.add(1, new DamageInfo(this, 10));
        damage.add(2, new DamageInfo(this, 16));
    }

    @Override
    public void usePreBattleAction()
    {
        CardCrawlGame.music.unsilenceBGM();
        AbstractDungeon.scene.fadeOutAmbiance();
        AbstractDungeon.getCurrRoom().playBgmInstantly("BOSS_CITY");
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new CursedLifePower(this, CURSE_AMT), CURSE_AMT));

        for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
            m.powers.add(new FakeDeathPower(m));
        }
    }

    @Override
    public void takeTurn()
    {
        switch (nextMove) {
            case SUMMON:
                for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                    if (m != this) {
                        if (!m.isDeadOrEscaped()) {
                            AbstractDungeon.actionManager.addToBottom(new VFXAction(new CollectorCurseEffect(m.hb.cX, m.hb.cY), 2.0F));
                        }
                        AbstractDungeon.actionManager.addToBottom(new SacrificeMinionAction(this, m));
                    }
                }
                break;
            case BUFF1:
                for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                    if (m != this && !m.isDeadOrEscaped()) {
                        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(m, this, new StrengthPower(m, STRENGTH_AMT), STRENGTH_AMT));
                    }
                }
                break;
            case BUFF2:
                for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                    if (m != this && !m.isDeadOrEscaped()) {
                        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(m, this, new IntangiblePower(m, INTANGIBLE_AMT), INTANGIBLE_AMT));
                    }
                }
                break;
            case DEBUFF1:
                AbstractDungeon.actionManager.addToBottom(new VFXAction(new CollectorCurseEffect(hb.cX, hb.cY), 2.0F));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new WeakPower(AbstractDungeon.player, MEGA_DEBUFF_AMT, true), MEGA_DEBUFF_AMT));
                //AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new VulnerablePower(AbstractDungeon.player, MEGA_DEBUFF_AMT, true), MEGA_DEBUFF_AMT));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new FrailPower(AbstractDungeon.player, MEGA_DEBUFF_AMT, true), MEGA_DEBUFF_AMT));
                break;
            case DEBUFF2:
                AbstractDungeon.actionManager.addToBottom(new MakeTempCardInDrawPileAction(new Void(), 1, true, true));
                break;
        }

        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    @Override
    protected void getMove(int num)
    {
        if (numTurns == 0) {
            setMove("Raise Dead", SUMMON, Intent.UNKNOWN);
        } else if (num < 50) {
            setMove(BUFF1, Intent.BUFF);
        } else if (num < 70) {
            setMove(DEBUFF2, Intent.DEBUFF);
        } else {
            setMove(DEBUFF1, Intent.STRONG_DEBUFF);
        }

        ++numTurns;
    }

    @Override
    public void update()
    {
        super.update();

        // TODO black particles
        if (!isDeadOrEscaped()) {
            particleTimer -= Gdx.graphics.getDeltaTime();
            if (particleTimer < 0) {
                particleTimer = PARTICAL_EMIT_INTERVAL;
                AbstractDungeon.topLevelEffectsQueue.add(new NecromanticTotemParticleEffect(hb.x + 88.0f * Settings.scale, hb.y + 207.0f * Settings.scale));
                AbstractDungeon.topLevelEffectsQueue.add(new NecromanticTotemParticleEffect(hb.x + 130.0f * Settings.scale, hb.y + 205.0f * Settings.scale));
            }
        }
    }

    @Override
    public void die()
    {
        useFastShakeAnimation(5.0F);
        CardCrawlGame.screenShake.rumble(4.0F);
        deathTimer += 1.5F;
        super.die();
        onBossVictoryLogic();
    }

    private float renderTimer = 0;
    @Override
    public void render(SpriteBatch sb)
    {
        animX = 0;
        animY = 0;
        renderImpl(sb, false);
        if (MathUtils.random(100) < 10) {
            animX = MathUtils.random(-10.0f, 5.0f);
            if (animX <= -9.9f) {
                animX = -15.0f;
            } else if (animX < -5.0f) {
                animX = -5.0f;
            }
            animX *= Settings.scale;
            animY = MathUtils.random(-3.0f, 3.0f) * Settings.scale;
        }
        renderImpl(sb, true);
    }

    private void renderImpl(SpriteBatch sb, boolean ghost)
    {
        renderTimer += Gdx.graphics.getDeltaTime() * MathUtils.random(0.5f, 2.0f);
        if (ghost) {
            sb.end();
            shader.begin();
            shader.setUniformf("timer", renderTimer);
            sb.setShader(shader);
            sb.begin();
        }

        super.render(sb);
    }

    @SpireOverride
    protected void renderIntentVfxBehind(SpriteBatch sb)
    {
        animX = 0;
        animY = 0;
        sb.end();
        shader.end();
        sb.setShader(null);
        sb.begin();

        SpireSuper.call(sb);
    }

    @Override
    public void renderHealth(SpriteBatch sb)
    {
        animX = 0;
        animY = 0;
        sb.end();
        shader.end();
        sb.setShader(null);
        sb.begin();

        super.renderHealth(sb);
    }
}