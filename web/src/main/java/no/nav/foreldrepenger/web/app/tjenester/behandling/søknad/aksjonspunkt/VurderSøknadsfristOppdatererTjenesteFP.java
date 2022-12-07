package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
public class VurderSøknadsfristOppdatererTjenesteFP extends VurderSøknadsfristOppdatererTjeneste {

    private YtelsesFordelingRepository ytelsesFordelingRepository;

    @Inject
    public VurderSøknadsfristOppdatererTjenesteFP(HistorikkTjenesteAdapter historikkAdapter,
                                                  BehandlingRepositoryProvider repositoryProvider,
                                                  YtelsesFordelingRepository ytelsesFordelingRepository) {
        super(historikkAdapter, repositoryProvider);
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    VurderSøknadsfristOppdatererTjenesteFP() {
        //CDI
    }

    @Override
    protected void lagreYtelseSpesifikkeData(Long behandlingId, Uttaksperiodegrense uttaksperiodegrense) {
        oppdaterYtelseFordelingMedMottattDato(behandlingId, uttaksperiodegrense.getMottattDato());
    }

    private void oppdaterYtelseFordelingMedMottattDato(Long behandlingId, LocalDate mottattDato) {
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(behandlingId);
        var eksisterendeOppgittFordeling = ytelseFordelingAggregat.getJustertFordeling().orElseThrow();
        var nyOppgittFordelingPerioder = eksisterendeOppgittFordeling.getPerioder().stream()
            .map(p -> OppgittPeriodeBuilder.fraEksisterende(p)
                .medMottattDato(mottattDato)
                .medTidligstMottattDato(utledTidligstMottattDato(p, mottattDato))
                .build())
            .collect(Collectors.toList());
        var nyOppgittFordeling = new OppgittFordelingEntitet(nyOppgittFordelingPerioder, eksisterendeOppgittFordeling.getErAnnenForelderInformert(),
            eksisterendeOppgittFordeling.ønskerJustertVedFødsel());
        var yfBuilder = YtelseFordelingAggregat.oppdatere(Optional.of(ytelseFordelingAggregat))
            .medOppgittFordeling(nyOppgittFordeling);
        ytelsesFordelingRepository.lagre(behandlingId, yfBuilder.build());
    }

    private LocalDate utledTidligstMottattDato(OppgittPeriodeEntitet periode, LocalDate mottattdato) {
        return periode.getTidligstMottattDato()
            .filter(d -> d.isBefore(periode.getMottattDato()) && d.isBefore(mottattdato))
            .orElse(mottattdato);
    }
}
