package no.nav.foreldrepenger.behandling.steg.avklarfakta.svp;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;

@ApplicationScoped
class LagreNyeTilretteleggingerTjeneste {

    private SvangerskapspengerRepository svangerskapspengerRepository;

    LagreNyeTilretteleggingerTjeneste() {
        // CDI
    }

    @Inject
    LagreNyeTilretteleggingerTjeneste(SvangerskapspengerRepository svangerskapspengerRepository) {
        this.svangerskapspengerRepository = svangerskapspengerRepository;
    }

    public void lagre(Behandling behandling, List<SvpTilretteleggingEntitet> nyeTilrettelegginger) {
        var svpGrunnlag = svangerskapspengerRepository.hentGrunnlag(behandling.getId()).orElseThrow(
            () -> new IllegalStateException("Fant ikke forventet grunnlag for behandling " + behandling.getId()));
        var nyttSvpGrunnlag = new SvpGrunnlagEntitet.Builder(svpGrunnlag)
            .medOverstyrteTilrettelegginger(nyeTilrettelegginger)
            .build();
        svangerskapspengerRepository.lagreOgFlush(nyttSvpGrunnlag);
    }

}
