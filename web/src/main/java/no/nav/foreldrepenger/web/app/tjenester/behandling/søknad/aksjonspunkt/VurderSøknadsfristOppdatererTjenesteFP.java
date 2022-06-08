package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
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
        var eksisterendeJustertFordeling = ytelseFordelingAggregat.getJustertFordeling().orElseThrow();
        var nyeJustertFordelingPerioder = eksisterendeJustertFordeling.getOppgittePerioder().stream()
            .map(p -> {
                var builder = OppgittPeriodeBuilder.fraEksisterende(p);
                if (Objects.equals(p.getPeriodeKilde(), FordelingPeriodeKilde.SØKNAD)) {
                    builder.medMottattDato(mottattDato);
                }
                if (Objects.equals(p.getPeriodeKilde(), FordelingPeriodeKilde.SØKNAD) &&
                    p.getTidligstMottattDato().filter(d -> d.isBefore(p.getMottattDato())).isEmpty() && 
                    p.getTidligstMottattDato().filter(d -> d.isBefore(mottattDato)).isEmpty()) {
                    builder.medTidligstMottattDato(mottattDato);
                }
                return builder.build();
            })
            .collect(Collectors.toList());
        var nyJustertFordeling = new OppgittFordelingEntitet(nyeJustertFordelingPerioder, eksisterendeJustertFordeling.getErAnnenForelderInformert());
        var yfBuilder = YtelseFordelingAggregat.oppdatere(Optional.of(ytelseFordelingAggregat))
            .medJustertFordeling(nyJustertFordeling);
        ytelsesFordelingRepository.lagre(behandlingId, yfBuilder.build());
    }
}
