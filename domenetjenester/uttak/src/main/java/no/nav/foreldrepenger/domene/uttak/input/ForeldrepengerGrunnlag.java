package no.nav.foreldrepenger.domene.uttak.input;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;

public class ForeldrepengerGrunnlag implements YtelsespesifiktGrunnlag {

    private boolean berørtBehandling;
    private FamilieHendelser familieHendelser;
    private OriginalBehandling originalBehandling;
    private Annenpart annenpart;
    private boolean oppgittAnnenForelderHarEngangsstønadForSammeBarn;
    private PleiepengerGrunnlagEntitet pleiepengerGrunnlag;
    private boolean oppdagetPleiepengerOverlappendeUtbetaling;
    private UføretrygdGrunnlagEntitet uføretrygdGrunnlag;
    private NesteSakGrunnlagEntitet nesteSakGrunnlag;
    private boolean dødsfall;
    private AktivitetskravGrunnlagEntitet aktivitetskravGrunnlag;
    private boolean mottattMorsArbeidDokument;
    private EøsUttakGrunnlagEntitet eøsUttakGrunnlag;

    public ForeldrepengerGrunnlag() {

    }

    public ForeldrepengerGrunnlag(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        this.berørtBehandling = foreldrepengerGrunnlag.berørtBehandling;
        this.familieHendelser = foreldrepengerGrunnlag.familieHendelser;
        this.originalBehandling = foreldrepengerGrunnlag.originalBehandling;
        this.annenpart = foreldrepengerGrunnlag.annenpart;
        this.oppgittAnnenForelderHarEngangsstønadForSammeBarn = foreldrepengerGrunnlag.oppgittAnnenForelderHarEngangsstønadForSammeBarn;
        this.pleiepengerGrunnlag = foreldrepengerGrunnlag.pleiepengerGrunnlag;
        this.oppdagetPleiepengerOverlappendeUtbetaling = foreldrepengerGrunnlag.oppdagetPleiepengerOverlappendeUtbetaling;
        this.uføretrygdGrunnlag = foreldrepengerGrunnlag.uføretrygdGrunnlag;
        this.nesteSakGrunnlag = foreldrepengerGrunnlag.nesteSakGrunnlag;
        this.dødsfall = foreldrepengerGrunnlag.dødsfall;
        this.aktivitetskravGrunnlag = foreldrepengerGrunnlag.aktivitetskravGrunnlag;
        this.mottattMorsArbeidDokument = foreldrepengerGrunnlag.mottattMorsArbeidDokument;
        this.eøsUttakGrunnlag = foreldrepengerGrunnlag.eøsUttakGrunnlag;
    }

    public boolean isBerørtBehandling() {
        return berørtBehandling;
    }

    public boolean isOppgittAnnenForelderHarEngangsstønadForSammeBarn() {
        return oppgittAnnenForelderHarEngangsstønadForSammeBarn;
    }

    public FamilieHendelser getFamilieHendelser() {
        return familieHendelser;
    }

    public Optional<OriginalBehandling> getOriginalBehandling() {
        return Optional.ofNullable(originalBehandling);
    }

    public Optional<Annenpart> getAnnenpart() {
        return Optional.ofNullable(annenpart);
    }

    public Optional<PleiepengerGrunnlagEntitet> getPleiepengerGrunnlag() {
        return Optional.ofNullable(pleiepengerGrunnlag);
    }

    public boolean isOppdagetPleiepengerOverlappendeUtbetaling() {
        return oppdagetPleiepengerOverlappendeUtbetaling;
    }

    public Optional<UføretrygdGrunnlagEntitet> getUføretrygdGrunnlag() {
        return Optional.ofNullable(uføretrygdGrunnlag);
    }

    public Optional<NesteSakGrunnlagEntitet> getNesteSakGrunnlag() {
        return Optional.ofNullable(nesteSakGrunnlag);
    }

    public boolean isDødsfall() {
        return dødsfall;
    }

    public Optional<AktivitetskravGrunnlagEntitet> getAktivitetskravGrunnlag() {
        return Optional.ofNullable(aktivitetskravGrunnlag);
    }

    public boolean isMottattMorsArbeidDokument() {
        return mottattMorsArbeidDokument;
    }

    public Optional<EøsUttakGrunnlagEntitet> getEøsUttakGrunnlag() {
        return Optional.ofNullable(eøsUttakGrunnlag);
    }

    public ForeldrepengerGrunnlag medErBerørtBehandling(boolean berørtBehandling) {
        var nyttGrunnlag = new ForeldrepengerGrunnlag(this);
        nyttGrunnlag.berørtBehandling = berørtBehandling;
        return nyttGrunnlag;
    }

    public ForeldrepengerGrunnlag medOppgittAnnenForelderHarEngangsstønadForSammeBarn(boolean harEngangsstønad) {
        var nyttGrunnlag = new ForeldrepengerGrunnlag(this);
        nyttGrunnlag.oppgittAnnenForelderHarEngangsstønadForSammeBarn = harEngangsstønad;
        return nyttGrunnlag;
    }

    public ForeldrepengerGrunnlag medFamilieHendelser(FamilieHendelser familieHendelser) {
        var nyttGrunnlag = new ForeldrepengerGrunnlag(this);
        nyttGrunnlag.familieHendelser = familieHendelser;
        return nyttGrunnlag;
    }

    public ForeldrepengerGrunnlag medOriginalBehandling(OriginalBehandling originalBehandling) {
        var nyttGrunnlag = new ForeldrepengerGrunnlag(this);
        nyttGrunnlag.originalBehandling = originalBehandling;
        return nyttGrunnlag;
    }

    public ForeldrepengerGrunnlag medAnnenpart(Annenpart annenpart) {
        var nyttGrunnlag = new ForeldrepengerGrunnlag(this);
        nyttGrunnlag.annenpart = annenpart;
        return nyttGrunnlag;
    }

    public ForeldrepengerGrunnlag medPleiepengerGrunnlag(PleiepengerGrunnlagEntitet pleiepengerGrunnlag) {
        var nyttGrunnlag = new ForeldrepengerGrunnlag(this);
        nyttGrunnlag.pleiepengerGrunnlag = pleiepengerGrunnlag;
        return nyttGrunnlag;
    }

    public ForeldrepengerGrunnlag medOppdagetPleiepengerOverlappendeUtbetaling(boolean overlapp) {
        var nyttGrunnlag = new ForeldrepengerGrunnlag(this);
        nyttGrunnlag.oppdagetPleiepengerOverlappendeUtbetaling = overlapp;
        return nyttGrunnlag;
    }

    public ForeldrepengerGrunnlag medUføretrygdGrunnlag(UføretrygdGrunnlagEntitet uføretrygdGrunnlag) {
        var nyttGrunnlag = new ForeldrepengerGrunnlag(this);
        nyttGrunnlag.uføretrygdGrunnlag = uføretrygdGrunnlag;
        return nyttGrunnlag;
    }

    public ForeldrepengerGrunnlag medNesteSakGrunnlag(NesteSakGrunnlagEntitet nesteSakGrunnlag) {
        var nyttGrunnlag = new ForeldrepengerGrunnlag(this);
        nyttGrunnlag.nesteSakGrunnlag = nesteSakGrunnlag;
        return nyttGrunnlag;
    }

    public ForeldrepengerGrunnlag medDødsfall(boolean dødsfall) {
        var nyttGrunnlag = new ForeldrepengerGrunnlag(this);
        nyttGrunnlag.dødsfall = dødsfall;
        return nyttGrunnlag;
    }

    public ForeldrepengerGrunnlag medAktivitetskravGrunnlag(AktivitetskravGrunnlagEntitet aktivitetskravGrunnlag) {
        var nyttGrunnlag = new ForeldrepengerGrunnlag(this);
        nyttGrunnlag.aktivitetskravGrunnlag = aktivitetskravGrunnlag;
        return nyttGrunnlag;
    }

    public ForeldrepengerGrunnlag medMottattMorsArbeidDokument(boolean mottattMorsArbeidDokument) {
        var nyttGrunnlag = new ForeldrepengerGrunnlag(this);
        nyttGrunnlag.mottattMorsArbeidDokument = mottattMorsArbeidDokument;
        return nyttGrunnlag;
    }

    public ForeldrepengerGrunnlag medEøsUttakGrunnlag(EøsUttakGrunnlagEntitet eøsUttakGrunnlag) {
        var nyttGrunnlag = new ForeldrepengerGrunnlag(this);
        nyttGrunnlag.eøsUttakGrunnlag = eøsUttakGrunnlag;
        return nyttGrunnlag;
    }
}
