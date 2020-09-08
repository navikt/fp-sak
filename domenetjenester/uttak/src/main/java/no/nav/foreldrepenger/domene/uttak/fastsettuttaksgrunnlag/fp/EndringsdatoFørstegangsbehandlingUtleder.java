package no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.fp;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;

@ApplicationScoped
public class EndringsdatoFørstegangsbehandlingUtleder {

    private YtelsesFordelingRepository ytelsesFordelingRepository;

    EndringsdatoFørstegangsbehandlingUtleder() {
        //CDI
    }

    @Inject
    public EndringsdatoFørstegangsbehandlingUtleder(YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    public LocalDate utledEndringsdato(Long behandlingId, List<OppgittPeriodeEntitet> oppgittePerioder) {
        YtelseFordelingAggregat ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(behandlingId);
        Optional<LocalDate> optionalFørsteSøkteUttaksdato = OppgittPeriodeUtil.finnFørsteSøkteUttaksdato(oppgittePerioder);
        if (optionalFørsteSøkteUttaksdato.isEmpty()) {
            throw new IllegalArgumentException("Utvikler-feil: Dette skal ikke skje. Ingen perioder i førstegangsøknad.");
        }
        LocalDate førsteSøkteUttaksdato = optionalFørsteSøkteUttaksdato.get();
        var manueltSattFørsteUttaksdato = ytelseFordelingAggregat.getAvklarteDatoer().map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato).orElse(førsteSøkteUttaksdato);
        return manueltSattFørsteUttaksdato.isBefore(førsteSøkteUttaksdato) ? manueltSattFørsteUttaksdato : førsteSøkteUttaksdato;
    }
}
