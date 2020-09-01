package no.nav.foreldrepenger.domene.uttak.input;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;

public class SvangerskapspengerGrunnlag implements YtelsespesifiktGrunnlag {

    private FamilieHendelse familieHendelse;
    private SvpGrunnlagEntitet svpGrunnlagEntitet;

    public SvangerskapspengerGrunnlag() {

    }

    public SvangerskapspengerGrunnlag(SvangerskapspengerGrunnlag svangerskapspengerGrunnlag) {
        familieHendelse = svangerskapspengerGrunnlag.familieHendelse;
        svpGrunnlagEntitet = svangerskapspengerGrunnlag.svpGrunnlagEntitet;
    }

    public FamilieHendelse getFamilieHendelse() {
        return familieHendelse;
    }

    public Optional<SvpGrunnlagEntitet> getGrunnlagEntitet() {
        return Optional.ofNullable(svpGrunnlagEntitet);
    }

    public SvangerskapspengerGrunnlag medFamilieHendelse(FamilieHendelse familieHendelse) {
        var nyttGrunnlag = new SvangerskapspengerGrunnlag(this);
        nyttGrunnlag.familieHendelse = familieHendelse;
        return nyttGrunnlag;
    }

    public SvangerskapspengerGrunnlag medSvpGrunnlagEntitet(SvpGrunnlagEntitet entitet) {
        var nyttGrunnlag = new SvangerskapspengerGrunnlag(this);
        nyttGrunnlag.svpGrunnlagEntitet = entitet;
        return nyttGrunnlag;
    }
}
