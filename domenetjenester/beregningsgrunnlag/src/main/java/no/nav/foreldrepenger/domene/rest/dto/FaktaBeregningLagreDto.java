package no.nav.foreldrepenger.domene.rest.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.validering.ValidKodeverk;

public class FaktaBeregningLagreDto {

    @Valid
    private VurderNyoppstartetFLDto vurderNyoppstartetFL;
    @Valid
    private VurderTidsbegrensetArbeidsforholdDto vurderTidsbegrensetArbeidsforhold;
    @Valid
    private VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto vurderNyIArbeidslivet;
    @Valid
    private FastsettMånedsinntektFLDto fastsettMaanedsinntektFL;
    @Valid
    private VurderLønnsendringDto vurdertLonnsendring;
    @Valid
    private FastsettMånedsinntektUtenInntektsmeldingDto fastsattUtenInntektsmelding;
    @Valid
    private VurderATogFLiSammeOrganisasjonDto vurderATogFLiSammeOrganisasjon;
    @Valid
    private BesteberegningFødendeKvinneDto besteberegningAndeler;
    @Valid
    private List<@ValidKodeverk FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller;
    @Valid
    private FastsettBgKunYtelseDto kunYtelseFordeling;
    @Valid
    private VurderEtterlønnSluttpakkeDto vurderEtterlønnSluttpakke;
    @Valid
    private FastsettEtterlønnSluttpakkeDto fastsettEtterlønnSluttpakke;
    @Valid
    private MottarYtelseDto mottarYtelse;
    @Valid
    private VurderMilitærDto vurderMilitaer;
    @Valid
    @Size(max = 100)
    private List<RefusjonskravPrArbeidsgiverVurderingDto> refusjonskravGyldighet;
    @Valid
    private FastsettInntektForArbeidUnderAAPDto fastsettInntektForArbeidUnderAAP;

    FaktaBeregningLagreDto() {
        // For Jackson
    }

    public FaktaBeregningLagreDto(List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller) {
        this.faktaOmBeregningTilfeller = faktaOmBeregningTilfeller;
    }

    public FaktaBeregningLagreDto(List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller, FastsettBgKunYtelseDto kunYtelseFordeling) {
        this.faktaOmBeregningTilfeller = faktaOmBeregningTilfeller;
        this.kunYtelseFordeling = kunYtelseFordeling;
    }

    public FaktaBeregningLagreDto(List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller, VurderTidsbegrensetArbeidsforholdDto vurderTidsbegrensetArbeidsforhold) {
        this.faktaOmBeregningTilfeller = faktaOmBeregningTilfeller;
        this.vurderTidsbegrensetArbeidsforhold = vurderTidsbegrensetArbeidsforhold;
    }

    public List<RefusjonskravPrArbeidsgiverVurderingDto> getRefusjonskravGyldighet() {
        return refusjonskravGyldighet;
    }

    public void setRefusjonskravGyldighet(List<RefusjonskravPrArbeidsgiverVurderingDto> refusjonskravGyldighet) {
        this.refusjonskravGyldighet = refusjonskravGyldighet;
    }

    public VurderNyoppstartetFLDto getVurderNyoppstartetFL() {
        return vurderNyoppstartetFL;
    }

    public void setVurderNyoppstartetFL(VurderNyoppstartetFLDto vurderNyoppstartetFL) {
        this.vurderNyoppstartetFL = vurderNyoppstartetFL;
    }

    public VurderTidsbegrensetArbeidsforholdDto getVurderTidsbegrensetArbeidsforhold() {
        return vurderTidsbegrensetArbeidsforhold;
    }

    public void setVurderTidsbegrensetArbeidsforhold(VurderTidsbegrensetArbeidsforholdDto vurderTidsbegrensetArbeidsforhold) {
        this.vurderTidsbegrensetArbeidsforhold = vurderTidsbegrensetArbeidsforhold;
    }

    public VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto getVurderNyIArbeidslivet() {
        return vurderNyIArbeidslivet;
    }

    public void setVurderNyIArbeidslivet(VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto vurderNyIArbeidslivet) {
        this.vurderNyIArbeidslivet = vurderNyIArbeidslivet;
    }

    public FastsettMånedsinntektFLDto getFastsettMaanedsinntektFL() {
        return fastsettMaanedsinntektFL;
    }

    public void setFastsettMaanedsinntektFL(FastsettMånedsinntektFLDto fastsettMaanedsinntektFL) {
        this.fastsettMaanedsinntektFL = fastsettMaanedsinntektFL;
    }

    public List<FaktaOmBeregningTilfelle> getFaktaOmBeregningTilfeller() {
        return faktaOmBeregningTilfeller;
    }

    public void setFaktaOmBeregningTilfeller(List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller) {
        this.faktaOmBeregningTilfeller = faktaOmBeregningTilfeller;
    }

    public VurderLønnsendringDto getVurdertLonnsendring() {
        return vurdertLonnsendring;
    }

    public void setVurdertLonnsendring(VurderLønnsendringDto vurdertLonnsendring) {
        this.vurdertLonnsendring = vurdertLonnsendring;
    }

    public VurderATogFLiSammeOrganisasjonDto getVurderATogFLiSammeOrganisasjon() {
        return vurderATogFLiSammeOrganisasjon;
    }

    public void setVurderATogFLiSammeOrganisasjon(VurderATogFLiSammeOrganisasjonDto vurderATogFLiSammeOrganisasjon) {
        this.vurderATogFLiSammeOrganisasjon = vurderATogFLiSammeOrganisasjon;
    }

    public BesteberegningFødendeKvinneDto getBesteberegningAndeler() {
        return besteberegningAndeler;
    }

    public void setBesteberegningAndeler(BesteberegningFødendeKvinneDto besteberegningAndeler) {
        this.besteberegningAndeler = besteberegningAndeler;
    }

    public FastsettBgKunYtelseDto getKunYtelseFordeling() {
        return kunYtelseFordeling;
    }

    public void setKunYtelseFordeling(FastsettBgKunYtelseDto kunYtelseFordeling) {
        this.kunYtelseFordeling = kunYtelseFordeling;
    }

    public VurderEtterlønnSluttpakkeDto getVurderEtterlønnSluttpakke() {
        return vurderEtterlønnSluttpakke;
    }

    public void setVurderEtterlønnSluttpakke(VurderEtterlønnSluttpakkeDto vurderEtterlønnSluttpakke) {
        this.vurderEtterlønnSluttpakke = vurderEtterlønnSluttpakke;
    }

    public FastsettEtterlønnSluttpakkeDto getFastsettEtterlønnSluttpakke() {
        return fastsettEtterlønnSluttpakke;
    }

    public void setFastsettEtterlønnSluttpakke(FastsettEtterlønnSluttpakkeDto fastsettEtterlønnSluttpakke) {
        this.fastsettEtterlønnSluttpakke = fastsettEtterlønnSluttpakke;
    }

    public void setMottarYtelse(MottarYtelseDto mottarYtelse) {
        this.mottarYtelse = mottarYtelse;
    }

    public MottarYtelseDto getMottarYtelse() {
        return mottarYtelse;
    }

    public FastsettMånedsinntektUtenInntektsmeldingDto getFastsattUtenInntektsmelding() {
        return fastsattUtenInntektsmelding;
    }

    public void setFastsattUtenInntektsmelding(FastsettMånedsinntektUtenInntektsmeldingDto fastsattUtenInntektsmelding) {
        this.fastsattUtenInntektsmelding = fastsattUtenInntektsmelding;
    }

    public VurderMilitærDto getVurderMilitaer() {
        return vurderMilitaer;
    }

    public void setVurderMilitaer(VurderMilitærDto vurderMilitaer) {
        this.vurderMilitaer = vurderMilitaer;
    }

    public FastsettInntektForArbeidUnderAAPDto getFastsettInntektForArbeidUnderAAP() {
        return fastsettInntektForArbeidUnderAAP;
    }

    public void setFastsettInntektForArbeidUnderAAP(FastsettInntektForArbeidUnderAAPDto fastsettInntektForArbeidUnderAAP) {
        this.fastsettInntektForArbeidUnderAAP = fastsettInntektForArbeidUnderAAP;
    }
}
