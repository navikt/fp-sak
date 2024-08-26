package no.nav.foreldrepenger.domene.aksjonspunkt;

import java.util.Collections;
import java.util.List;
import java.util.Optional;


/**
 * Endringsobjekt for fakta-vurderinger i fakta om beregning.
 */
public class FaktaOmBeregningVurderinger {

    /**
     * Gjelder fakta: om en person har etterlønn/sluttpakke
     */
    private ToggleEndring harEtterlønnSluttpakkeEndring;

    /**
     * Gjelder fakta: om en person har hatt lønnsendring i beregningsperioden
     */
    private ToggleEndring harLønnsendringIBeregningsperiodenEndring;

    /**
     * Gjelder fakta: om en person som har oppgitt militær og siviltjeneste har dette som gyldig aktivitet
     */
    private ToggleEndring harMilitærSiviltjenesteEndring;

    /**
     * Gjelder fakta: om en person som har oppgitt å være selvstendig også er ny i arbeidslivet
     */
    private ToggleEndring erSelvstendingNyIArbeidslivetEndring;

    /**
     * Gjelder fakta: om en person som har oppgitt å vere frilans også er nyoppstartet
     */
    private ToggleEndring erNyoppstartetFLEndring;

    /**
     * Gjelder fakta: om det er mottatt ytelse for frilans eller arbeidsaktivitet uten inntektsmelding
     */
    private List<ErMottattYtelseEndring> erMottattYtelseEndringer;

    /**
     * Gjelder fakta: om et arbeidsforhold er tidsbegrenset
     */
    private List<ErTidsbegrensetArbeidsforholdEndring> erTidsbegrensetArbeidsforholdEndringer;

    /**
     * Gjelder fakta: om et refusjonskrav som etter reglene har kommet inn for sent likevel skal tas med i beregning
     */
    private List<RefusjonskravGyldighetEndring> vurderRefusjonskravGyldighetEndringer;

    public void setHarEtterlønnSluttpakkeEndring(ToggleEndring harEtterlønnSluttpakkeEndring) {
        this.harEtterlønnSluttpakkeEndring = harEtterlønnSluttpakkeEndring;
    }

    public void setHarLønnsendringIBeregningsperiodenEndring(ToggleEndring harLønnsendringIBeregningsperiodenEndring) {
        this.harLønnsendringIBeregningsperiodenEndring = harLønnsendringIBeregningsperiodenEndring;
    }

    public void setHarMilitærSiviltjenesteEndring(ToggleEndring harMilitærSiviltjenesteEndring) {
        this.harMilitærSiviltjenesteEndring = harMilitærSiviltjenesteEndring;
    }

    public void setErSelvstendingNyIArbeidslivetEndring(ToggleEndring erSelvstendingNyIArbeidslivetEndring) {
        this.erSelvstendingNyIArbeidslivetEndring = erSelvstendingNyIArbeidslivetEndring;
    }

    public void setErNyoppstartetFLEndring(ToggleEndring erNyoppstartetFLEndring) {
        this.erNyoppstartetFLEndring = erNyoppstartetFLEndring;
    }

    public void setErMottattYtelseEndringer(List<ErMottattYtelseEndring> erMottattYtelseEndringer) {
        this.erMottattYtelseEndringer = erMottattYtelseEndringer;
    }

    public void setErTidsbegrensetArbeidsforholdEndringer(List<ErTidsbegrensetArbeidsforholdEndring> erTidsbegrensetArbeidsforholdEndringer) {
        this.erTidsbegrensetArbeidsforholdEndringer = erTidsbegrensetArbeidsforholdEndringer;
    }

    public void setVurderRefusjonskravGyldighetEndringer(List<RefusjonskravGyldighetEndring> vurderRefusjonskravGyldighetEndringer) {
        this.vurderRefusjonskravGyldighetEndringer = vurderRefusjonskravGyldighetEndringer;
    }

    public Optional<ToggleEndring> getHarEtterlønnSluttpakkeEndring() {
        return Optional.ofNullable(harEtterlønnSluttpakkeEndring);
    }

    public Optional<ToggleEndring> getHarLønnsendringIBeregningsperiodenEndring() {
        return Optional.ofNullable(harLønnsendringIBeregningsperiodenEndring);
    }

    public Optional<ToggleEndring> getHarMilitærSiviltjenesteEndring() {
        return Optional.ofNullable(harMilitærSiviltjenesteEndring);
    }

    public Optional<ToggleEndring> getErSelvstendingNyIArbeidslivetEndring() {
        return Optional.ofNullable(erSelvstendingNyIArbeidslivetEndring);
    }

    public Optional<ToggleEndring> getErNyoppstartetFLEndring() {
        return Optional.ofNullable(erNyoppstartetFLEndring);
    }

    public List<ErMottattYtelseEndring> getErMottattYtelseEndringer() {
        return erMottattYtelseEndringer == null ? Collections.emptyList() : erMottattYtelseEndringer;
    }

    public List<ErTidsbegrensetArbeidsforholdEndring> getErTidsbegrensetArbeidsforholdEndringer() {
        return erTidsbegrensetArbeidsforholdEndringer == null ? Collections.emptyList() : erTidsbegrensetArbeidsforholdEndringer;
    }

    public List<RefusjonskravGyldighetEndring> getVurderRefusjonskravGyldighetEndringer() {
        return vurderRefusjonskravGyldighetEndringer == null ? Collections.emptyList() : vurderRefusjonskravGyldighetEndringer;
    }
}
