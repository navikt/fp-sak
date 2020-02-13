package no.nav.foreldrepenger.domene.uttak.input;

import java.util.Optional;

public class ForeldrepengerGrunnlag implements YtelsespesifiktGrunnlag {

    private int dekningsgrad = 100;
    private boolean tapendeBehandling;
    private FamilieHendelser familieHendelser;
    private OriginalBehandling originalBehandling;
    private Annenpart annenpart;

    public ForeldrepengerGrunnlag() {

    }

    public ForeldrepengerGrunnlag(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        dekningsgrad = foreldrepengerGrunnlag.dekningsgrad;
        tapendeBehandling = foreldrepengerGrunnlag.tapendeBehandling;
        familieHendelser = foreldrepengerGrunnlag.familieHendelser;
        originalBehandling = foreldrepengerGrunnlag.originalBehandling;
        annenpart = foreldrepengerGrunnlag.annenpart;
    }

    @Override
    public int getDekningsgrad() {
        return dekningsgrad;
    }

    public boolean isTapendeBehandling() {
        return tapendeBehandling;
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

    public ForeldrepengerGrunnlag medDekningsgrad(int dekningsgrad) {
        var nyttGrunnlag = new ForeldrepengerGrunnlag(this);
        nyttGrunnlag.dekningsgrad = dekningsgrad;
        return nyttGrunnlag;
    }

    public ForeldrepengerGrunnlag medErTapendeBehandling(boolean tapendeBehandling) {
        var nyttGrunnlag = new ForeldrepengerGrunnlag(this);
        nyttGrunnlag.tapendeBehandling = tapendeBehandling;
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
}
