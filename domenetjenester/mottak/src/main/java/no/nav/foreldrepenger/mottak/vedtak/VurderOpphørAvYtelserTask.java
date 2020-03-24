package no.nav.foreldrepenger.mottak.vedtak;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev.InformasjonssakRepository;
import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev.OverlappData;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.rest.LoggHistoriskOverlappFPInfotrygdVLTjeneste;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.rest.SjekkOverlappForeldrepengerInfotrygdTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(VurderOpphørAvYtelserTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOpphørAvYtelserTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "iverksetteVedtak.vurderOpphørAvYtelser";

    private static final Logger LOG = LoggerFactory.getLogger(VurderOpphørAvYtelserTask.class);
    public static final String HIJACK_KEY_KEY = "hijack";
    public static final String HIJACK_FOM_KEY = "fom";
    public static final String HIJACK_TOM_KEY = "tom";

    private InformasjonssakRepository informasjonssakRepository;
    private SjekkOverlappForeldrepengerInfotrygdTjeneste overlapper;
    private LoggHistoriskOverlappFPInfotrygdVLTjeneste loggertjeneste;



    private VurderOpphørAvYtelser tjeneste;

    VurderOpphørAvYtelserTask() {
        // for CDI proxy
    }

    @Inject
    public VurderOpphørAvYtelserTask(VurderOpphørAvYtelser tjeneste,
                                     InformasjonssakRepository informasjonssakRepository,
                                     LoggHistoriskOverlappFPInfotrygdVLTjeneste loggertjeneste,
                                     SjekkOverlappForeldrepengerInfotrygdTjeneste overlapper) {
        this.tjeneste = tjeneste;
        this.informasjonssakRepository = informasjonssakRepository;
        this.overlapper = overlapper;
        this.loggertjeneste = loggertjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        if (prosessTaskData.getPropertyValue(HIJACK_KEY_KEY) != null && HIJACK_KEY_KEY.equalsIgnoreCase(prosessTaskData.getPropertyValue(HIJACK_KEY_KEY))) {
            loggOverlapp(LocalDate.parse(prosessTaskData.getPropertyValue(HIJACK_FOM_KEY), DateTimeFormatter.ISO_LOCAL_DATE),
                LocalDate.parse(prosessTaskData.getPropertyValue(HIJACK_TOM_KEY), DateTimeFormatter.ISO_LOCAL_DATE));
            return;
        }
        Long behandlingId = prosessTaskData.getBehandlingId();
        tjeneste.vurderOpphørAvYtelser(prosessTaskData.getFagsakId(), behandlingId);
    }

    private void loggOverlapp(LocalDate fom, LocalDate tom) {
        var saker = informasjonssakRepository.finnSakerMedSisteVedtakOpprettetInnenIntervall(fom, tom);
        var funnetoverlapp = saker.stream().map(this::vurderOverlapp).filter(Objects::nonNull).collect(Collectors.toList());
        funnetoverlapp.forEach(o -> loggertjeneste.vurderOglagreEventueltOverlapp(o.data.getBehandlingId(), o.data.getAnnenPartAktørId(), o.olFPBR, o.olFPAP, o.olSVP));
    }

    public static class OverlappendeSak {
        public OverlappData data; // NOSONAR
        public String saksnummer;  // NOSONAR
        public String overlappene ; // NOSONAR
        public boolean olFPBR = false; // NOSONAR
        public boolean olFPAP = false; // NOSONAR
        public boolean olSVP = false; // NOSONAR


        @Override
        public String toString() {
            return "[ " + saksnummer + " overlappes " + overlappene + " ]";
        }
    }

    private OverlappendeSak vurderOverlapp(OverlappData data) {
        boolean match = false;
        LocalDate startdato = fomMandag(data.getMinUtbetalingDato());
        var resultat = new OverlappendeSak();
        var builder = new StringBuilder();
        resultat.saksnummer = data.getSaksnummer().getVerdi();
        if (overlapper.harForeldrepengerInfotrygdSomOverlapper(data.getAktørId(), startdato)) {
            match = true;
            resultat.olFPBR = true;
            builder.append(" Foreldrepenger ");
        }
        if (RelasjonsRolleType.erMor(data.getRolle()) && overlapper.harSvangerskapspengerInfotrygdSomOverlapper(data.getAktørId(), startdato)) {
            match = true;
            resultat.olSVP = true;
            builder.append(" Svangerskap ");
        }
        if (RelasjonsRolleType.erMor(data.getRolle()) && data.getAnnenPartAktørId() != null && FagsakYtelseType.FORELDREPENGER.equals(data.getYtelseType()) &&
            overlapper.harForeldrepengerInfotrygdSomOverlapper(data.getAnnenPartAktørId(), startdato)) {
            match = true;
            resultat.olFPAP = true;
            builder.append(" AnnenPart ");
        }
        if (!match)
            return null;
        resultat.overlappene = builder.toString();
        resultat.data = data;

        // TODO(jol) enten slett kode eller logg til OVERLAPP-fil
        LOG.info("FPSAK DETEKTOR {}", resultat);
        return resultat;
    }

    private LocalDate fomMandag(LocalDate fom) {
        DayOfWeek ukedag = DayOfWeek.from(fom);
        if (DayOfWeek.SUNDAY.getValue() == ukedag.getValue())
            return fom.plusDays(1);
        if (DayOfWeek.SATURDAY.getValue() == ukedag.getValue())
            return fom.plusDays(2);
        return fom;
    }
}
