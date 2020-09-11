package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@ApplicationScoped
public class OppgittPeriodeMottattDatoTjeneste {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @Inject
    public OppgittPeriodeMottattDatoTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
    }

    OppgittPeriodeMottattDatoTjeneste() {
        //CDI
    }

    /**
     * Henter mottatt dato for periode fra original behandling hvis den finnes
     */
    public Optional<LocalDate> finnMottattDatoForPeriode(Behandling behandling, OppgittPeriodeEntitet periode) {
        var originalBehandling = behandling.getOriginalBehandlingId();
        if (originalBehandling.isEmpty()) {
            return Optional.empty();
        }

        var matchendePerioderIOriginalBehandling = finnMatchendePerioder(periode, originalBehandling.get());
        if (matchendePerioderIOriginalBehandling.isEmpty()) {
            return Optional.empty();
        }

        if (matchendePerioderIOriginalBehandling.size() > 1) {
            throw new IllegalStateException("Finner mer enn 1 matchende oppgitt periode i original behandling" +
                " for periode" + periode.getFom() + " - " + periode.getTom());
        }
        return Optional.ofNullable(matchendePerioderIOriginalBehandling.get(0).getMottattDato());
    }

    private List<OppgittPeriodeEntitet> finnMatchendePerioder(OppgittPeriodeEntitet periode, Long originalBehandling) {
        return ytelseFordelingTjeneste.hentAggregat(originalBehandling)
            .getGjeldendeSøknadsperioder().getOppgittePerioder()
            .stream()
            .filter(p -> lik(periode, p))
            .collect(Collectors.toList());
    }

    private boolean lik(OppgittPeriodeEntitet periode1, OppgittPeriodeEntitet periode2) {
        var like = periode1.erOmsluttetAv(periode2)
            && Objects.equals(periode1.getÅrsak(), periode2.getÅrsak())
            && Objects.equals(periode1.getPeriodeType(), periode2.getPeriodeType())
            && Objects.equals(periode1.getSamtidigUttaksprosent(), periode2.getSamtidigUttaksprosent());
        if (like && periode1.erGradert()) {
            return periode2.erGradert() &&
                periode1.getErArbeidstaker() == periode2.getErArbeidstaker() &&
                Objects.equals(periode1.getArbeidsprosent(), periode2.getArbeidsprosent()) &&
                Objects.equals(periode1.getArbeidsgiver(), periode2.getArbeidsgiver());
        }
        return like;
    }
}
