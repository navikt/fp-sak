package no.nav.foreldrepenger.domene.uttak.input;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerGrunnlagEntitet;

public class ForeldrepengerGrunnlag implements YtelsespesifiktGrunnlag {

    private boolean berørtBehandling;
    private FamilieHendelser familieHendelser;
    private OriginalBehandling originalBehandling;
    private Annenpart annenpart;
    private PleiepengerGrunnlagEntitet pleiepengerGrunnlag;

    public ForeldrepengerGrunnlag() {

    }

    public ForeldrepengerGrunnlag(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        berørtBehandling = foreldrepengerGrunnlag.berørtBehandling;
        familieHendelser = foreldrepengerGrunnlag.familieHendelser;
        originalBehandling = foreldrepengerGrunnlag.originalBehandling;
        annenpart = foreldrepengerGrunnlag.annenpart;
        pleiepengerGrunnlag = foreldrepengerGrunnlag.pleiepengerGrunnlag;
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
}
