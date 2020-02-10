package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.Stønadskonto;
import no.nav.foreldrepenger.domene.uttak.UttakEnumMapper;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Konto;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Kontoer;

@ApplicationScoped
public class KontoerGrunnlagBygger {

    private FagsakRelasjonRepository fagsakRelasjonRepository;

    @Inject
    public KontoerGrunnlagBygger(UttakRepositoryProvider repositoryProvider) {
        fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
    }

    KontoerGrunnlagBygger() {
        //CDI
    }

    public Kontoer.Builder byggGrunnlag(BehandlingReferanse ref) {
        var stønadskontoer = hentStønadskontoer(ref);
        return new Kontoer.Builder()
            .medKontoList(stønadskontoer.stream().map(this::map).collect(Collectors.toList()));
    }

    private Konto.Builder map(Stønadskonto stønadskonto) {
        return new Konto.Builder()
            .medTrekkdager(stønadskonto.getMaxDager())
            .medType(UttakEnumMapper.map(stønadskonto.getStønadskontoType()));
    }

    private Set<Stønadskonto> hentStønadskontoer(BehandlingReferanse ref) {
        return fagsakRelasjonRepository.finnRelasjonFor(ref.getSaksnummer()).getGjeldendeStønadskontoberegning()
            .orElseThrow(() -> new IllegalArgumentException("Behandling mangler stønadskontoer"))
            .getStønadskontoer();
    }
}
