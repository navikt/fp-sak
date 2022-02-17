package no.nav.foreldrepenger.domene.uttak.input;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;

public class ForeldrepengerGrunnlag implements YtelsespesifiktGrunnlag {

    private boolean berørtBehandling;
    private FamilieHendelser familieHendelser;
    private OriginalBehandling originalBehandling;
    private Annenpart annenpart;
    private PleiepengerGrunnlagEntitet pleiepengerGrunnlag;
    private UføretrygdGrunnlagEntitet uføretrygdGrunnlag;
    private NesteSakGrunnlagEntitet nesteSakEntitet;

    public ForeldrepengerGrunnlag() {

    }

    public ForeldrepengerGrunnlag(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        this.berørtBehandling = foreldrepengerGrunnlag.berørtBehandling;
        this.familieHendelser = foreldrepengerGrunnlag.familieHendelser;
        this.originalBehandling = foreldrepengerGrunnlag.originalBehandling;
        this.annenpart = foreldrepengerGrunnlag.annenpart;
        this.pleiepengerGrunnlag = foreldrepengerGrunnlag.pleiepengerGrunnlag;
        this.uføretrygdGrunnlag = foreldrepengerGrunnlag.uføretrygdGrunnlag;
        this.nesteSakEntitet = foreldrepengerGrunnlag.nesteSakEntitet;
    }

    public boolean isBerørtBehandling() {
        return berørtBehandling;
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

    public Optional<UføretrygdGrunnlagEntitet> getUføretrygdGrunnlag() {
        return Optional.ofNullable(uføretrygdGrunnlag);
    }

    public Optional<NesteSakGrunnlagEntitet> getNesteSakGrunnlag() {
        return Optional.ofNullable(nesteSakEntitet);
    }

    public ForeldrepengerGrunnlag medErBerørtBehandling(boolean berørtBehandling) {
        var nyttGrunnlag = new ForeldrepengerGrunnlag(this);
        nyttGrunnlag.berørtBehandling = berørtBehandling;
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

    public ForeldrepengerGrunnlag medUføretrygdGrunnlag(UføretrygdGrunnlagEntitet uføretrygdGrunnlag) {
        var nyttGrunnlag = new ForeldrepengerGrunnlag(this);
        nyttGrunnlag.uføretrygdGrunnlag = uføretrygdGrunnlag;
        return nyttGrunnlag;
    }

    public ForeldrepengerGrunnlag medNesteSakGrunnlag(NesteSakGrunnlagEntitet nesteSakGrunnlag) {
        var nyttGrunnlag = new ForeldrepengerGrunnlag(this);
        nyttGrunnlag.nesteSakEntitet = nesteSakGrunnlag;
        return nyttGrunnlag;
    }
}
