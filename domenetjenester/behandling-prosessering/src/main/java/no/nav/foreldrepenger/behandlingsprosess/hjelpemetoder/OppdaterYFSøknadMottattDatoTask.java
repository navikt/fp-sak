package no.nav.foreldrepenger.behandlingsprosess.hjelpemetoder;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(OppdaterYFSøknadMottattDatoTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class OppdaterYFSøknadMottattDatoTask extends BehandlingProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(OppdaterYFSøknadMottattDatoTask.class);
    public static final String TASKTYPE = "oppdater.yf.soknad.mottattdato";

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private BehandlingLåsRepository behandlingLåsRepository;
    private SøknadRepository søknadRepository;
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;
    private BehandlingRepository behandlingRepository;
    private EntityManager entityManager;


    @Inject
    public OppdaterYFSøknadMottattDatoTask(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                           BehandlingLåsRepository behandlingLåsRepository,
                                           SøknadRepository søknadRepository,
                                           UttaksperiodegrenseRepository uttaksperiodegrenseRepository,
                                           BehandlingRepository behandlingRepository,
                                           EntityManager entityManager) {
        super(behandlingLåsRepository);
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.behandlingLåsRepository = behandlingLåsRepository;
        this.søknadRepository = søknadRepository;
        this.uttaksperiodegrenseRepository = uttaksperiodegrenseRepository;
        this.behandlingRepository = behandlingRepository;
        this.entityManager = entityManager;
    }

    OppdaterYFSøknadMottattDatoTask() {
        // CDI krav
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        LOG.info("Oppdaterer mottatt dato for behandling {}", behandling.getId());
        oppdaterMottattDatoForBehandling(behandling);
    }

    private void oppdaterMottattDatoForBehandling(Behandling behandling) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandling.getId());
        if (ytelseFordelingAggregat.isEmpty()) {
            return;
        }
        var behandlingLås = behandlingLåsRepository.taLås(behandling.getId());

        var oppgittFordeling = ytelseFordelingAggregat.get().getOppgittFordeling();
        var justertFordeling = ytelseFordelingAggregat.get().getJustertFordeling();
        var overstyrtFordeling = ytelseFordelingAggregat.get().getOverstyrtFordeling();

        oppgittFordeling.getOppgittePerioder().forEach(p -> oppdaterMottattDato(p, behandling));
        justertFordeling.ifPresent(f -> f.getOppgittePerioder().forEach(p -> oppdaterMottattDato(p, behandling)));
        overstyrtFordeling.ifPresent(f -> f.getOppgittePerioder().forEach(p -> oppdaterMottattDato(p, behandling)));
        behandlingLåsRepository.oppdaterLåsVersjon(behandlingLås);
        entityManager.flush();
    }

    private void oppdaterMottattDato(OppgittPeriodeEntitet periode, Behandling behandling) {
        var mottattDato = utledMottattDato(periode, behandling);
        if (mottattDato == null) {
            throw new IllegalStateException("Kunne ikke utlede mottatt dato for behandling " + behandling.getId());
        }
        var updated = entityManager.createNativeQuery("update YF_FORDELING_PERIODE set mottatt_dato_temp = :md where id=:periodeId")
            .setParameter("md", mottattDato)
            .setParameter("periodeId", periode.getId())
            .executeUpdate();
        if (updated != 1) {
            throw new RuntimeException("Ikke oppdatert");
        }
    }

    private LocalDate utledMottattDato(OppgittPeriodeEntitet periode, Behandling behandling) {
        var tidligstBehandlingMedPeriode = finnTidligstBehandling(periode, behandling);
        return hentMottattDatoFraBehandling(tidligstBehandlingMedPeriode);
    }

    private LocalDate hentMottattDatoFraBehandling(Behandling behandling) {
        if (behandlingHarLøstSøknadsfristAP(behandling)) {
            var uttaksperiodegrense = uttaksperiodegrenseRepository.hentHvisEksisterer(behandling.getId());
            return uttaksperiodegrense.orElseThrow().getMottattDato();
        }
        return hentMottattDatoFraSøknad(behandling);
    }

    private boolean behandlingHarLøstSøknadsfristAP(Behandling behandling) {
        var ap = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRIST);
        return ap.isPresent() && ap.get().erUtført() && ap.get().getOpprettetTidspunkt().isAfter(behandling.getOpprettetDato()) &&
            (behandling.getAvsluttetDato() == null || ap.get().getOpprettetTidspunkt().isBefore(behandling.getAvsluttetDato()));
    }

    private LocalDate hentMottattDatoFraSøknad(Behandling behandling) {
        var søknad = søknadRepository.hentSøknadHvisEksisterer(behandling.getId());
        if (søknad.isEmpty()) {
            return søknadRepository.hentFørstegangsSøknad(behandling).getMottattDato();
        }
        return søknad.get().getMottattDato();
    }

    private Behandling finnTidligstBehandling(OppgittPeriodeEntitet periode, Behandling behandling) {
        var originalBehandling = behandling.getOriginalBehandlingId().map(behandlingRepository::hentBehandling);
        var førsteBehandlingMedPeriode = behandling;
        while (originalBehandling.isPresent()) {
            var p = finnPeriodeIBehandling(periode, originalBehandling.get());
            if (p.isPresent()) {
                førsteBehandlingMedPeriode = originalBehandling.get();
            }
            originalBehandling = originalBehandling.get().getOriginalBehandlingId().map(behandlingRepository::hentBehandling);
        }
        return førsteBehandlingMedPeriode;
    }

    private Optional<OppgittPeriodeEntitet> finnPeriodeIBehandling(OppgittPeriodeEntitet periode, Behandling behandling) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandling.getId());
        if (ytelseFordelingAggregat.isEmpty()) {
            return Optional.empty();
        }
        var oppgittFordeling = ytelseFordelingAggregat.get().getOppgittFordeling();

        return oppgittFordeling.getOppgittePerioder().stream().filter(op -> lik(periode, op)).findFirst();
    }

    private boolean lik(OppgittPeriodeEntitet periode1, OppgittPeriodeEntitet periode2) {
        return erOmsluttetAv(periode1, periode2)
            && Objects.equals(periode1.getÅrsak(), periode2.getÅrsak())
            && Objects.equals(periode1.getArbeidsprosent(), periode2.getArbeidsprosent())
            && periode1.getErArbeidstaker() == periode2.getErArbeidstaker()
            && Objects.equals(periode1.getArbeidsgiver(), periode2.getArbeidsgiver())
            && Objects.equals(periode1.getPeriodeType(), periode2.getPeriodeType())
            && Objects.equals(periode1.getSamtidigUttaksprosent(), periode2.getSamtidigUttaksprosent())
            ;
    }

    private static boolean erOmsluttetAv(OppgittPeriodeEntitet periode1, OppgittPeriodeEntitet periode2) {
        return !periode2.getFom().isAfter(periode1.getFom()) && !periode2.getTom().isBefore(periode1.getTom());
    }
}
