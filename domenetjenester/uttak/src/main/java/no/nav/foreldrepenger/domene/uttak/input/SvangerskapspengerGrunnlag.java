package no.nav.foreldrepenger.domene.uttak.input;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;

public class SvangerskapspengerGrunnlag implements YtelsespesifiktGrunnlag {

    private FamilieHendelse familieHendelse;
    private SvpGrunnlagEntitet svpGrunnlagEntitet;
    private NesteSakGrunnlagEntitet nesteSakGrunnlagEntitet;

    public SvangerskapspengerGrunnlag() {

    }

    public SvangerskapspengerGrunnlag(SvangerskapspengerGrunnlag svangerskapspengerGrunnlag) {
        this.familieHendelse = svangerskapspengerGrunnlag.familieHendelse;
        this.svpGrunnlagEntitet = svangerskapspengerGrunnlag.svpGrunnlagEntitet;
        this.nesteSakGrunnlagEntitet = svangerskapspengerGrunnlag.nesteSakGrunnlagEntitet;
    }

    public FamilieHendelse getFamilieHendelse() {
        return familieHendelse;
    }

    public Optional<SvpGrunnlagEntitet> getGrunnlagEntitet() {
        return Optional.ofNullable(svpGrunnlagEntitet);
    }

    public Optional<NesteSakGrunnlagEntitet> nesteSakEntitet() {
        return Optional.ofNullable(nesteSakGrunnlagEntitet);
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

    public SvangerskapspengerGrunnlag medNesteSakEntitet(NesteSakGrunnlagEntitet entitet) {
        var nyttGrunnlag = new SvangerskapspengerGrunnlag(this);
        nyttGrunnlag.nesteSakGrunnlagEntitet = entitet;
        return nyttGrunnlag;
    }
}
