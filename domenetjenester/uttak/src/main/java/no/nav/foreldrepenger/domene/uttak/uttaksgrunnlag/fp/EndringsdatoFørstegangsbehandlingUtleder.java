package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;

import java.time.LocalDate;
import java.util.List;

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
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(behandlingId);
        var optionalFørsteSøkteUttaksdato = OppgittPeriodeUtil.finnFørsteSøkteUttaksdato(oppgittePerioder);
        if (optionalFørsteSøkteUttaksdato.isEmpty()) {
            throw new IllegalArgumentException(
                "Utvikler-feil: Dette skal ikke skje. Ingen perioder i førstegangsøknad.");
        }
        var førsteSøkteUttaksdato = optionalFørsteSøkteUttaksdato.get();
        var manueltSattFørsteUttaksdato = ytelseFordelingAggregat.getAvklarteDatoer()
            .map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato)
            .orElse(førsteSøkteUttaksdato);
        return manueltSattFørsteUttaksdato.isBefore(førsteSøkteUttaksdato) ?
            manueltSattFørsteUttaksdato : førsteSøkteUttaksdato;
    }
}
