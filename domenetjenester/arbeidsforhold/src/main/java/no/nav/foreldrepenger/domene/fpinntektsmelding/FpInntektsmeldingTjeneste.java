package no.nav.foreldrepenger.domene.fpinntektsmelding;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.tilMaskertNummer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrganisasjonsNummerValidator;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ApplicationScoped
public class FpInntektsmeldingTjeneste {
    private FpinntektsmeldingKlient klient;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private HistorikkRepository historikkRepo;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    private static final Logger LOG = LoggerFactory.getLogger(FpInntektsmeldingTjeneste.class);

    public FpInntektsmeldingTjeneste() {
        // CDI
    }

    @Inject
    public FpInntektsmeldingTjeneste(FpinntektsmeldingKlient klient,
                                     ProsessTaskTjeneste prosessTaskTjeneste,
                                     SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                     HistorikkRepository historikkRepo,
                                     ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.klient = klient;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.historikkRepo = historikkRepo;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    public void lagForespørselTask(String ag, BehandlingReferanse ref) {
        var taskdata = ProsessTaskData.forTaskType(TaskType.forProsessTask(FpinntektsmeldingTask.class));
        taskdata.setBehandling(ref.fagsakId(), ref.behandlingId());
        taskdata.setCallIdFraEksisterende();
        taskdata.setProperty(FpinntektsmeldingTask.ARBEIDSGIVER_KEY, ag);
        prosessTaskTjeneste.lagre(taskdata);
    }

    public void overstyrInntektsmelding(Inntektsmelding inntektsmeldingSomSkalOverstyres,
                                        LocalDate opphørFom,
                                        String saksbehandlerIdent,
                                        BehandlingReferanse ref) {
        var ytelse = ref.fagsakYtelseType().equals(FagsakYtelseType.FORELDREPENGER) ? OverstyrInntektsmeldingRequest.YtelseType.FORELDREPENGER : OverstyrInntektsmeldingRequest.YtelseType.SVANGERSKAPSPENGER;
        var startdato = inntektsmeldingSomSkalOverstyres.getStartDatoPermisjon()
            .orElseGet(() -> skjæringstidspunktTjeneste.getSkjæringstidspunkter(ref.behandlingId()).getUtledetSkjæringstidspunkt());
        var refusjon = Optional.ofNullable(inntektsmeldingSomSkalOverstyres.getRefusjonBeløpPerMnd()).map(Beløp::getVerdi).orElse(null);
        var arbeidsgiver = new OverstyrInntektsmeldingRequest.ArbeidsgiverDto(
            inntektsmeldingSomSkalOverstyres.getArbeidsgiver().getIdentifikator());
        var aktørId = new OverstyrInntektsmeldingRequest.AktørIdDto(ref.aktørId().getId());
        var endringerIRefusjon = mapRefusjonsendringer(inntektsmeldingSomSkalOverstyres.getEndringerRefusjon(), opphørFom);
        var naturalytelser = mapNaturalytelser(inntektsmeldingSomSkalOverstyres.getNaturalYtelser());
        var request = new OverstyrInntektsmeldingRequest(aktørId, arbeidsgiver, startdato, ytelse,
            inntektsmeldingSomSkalOverstyres.getInntektBeløp().getVerdi(), refusjon, endringerIRefusjon, naturalytelser, saksbehandlerIdent, new SaksnummerDto(ref.saksnummer().getVerdi()));
        klient.overstyrInntektsmelding(request);
    }

    private List<OverstyrInntektsmeldingRequest.BortfaltNaturalytelseRequestDto> mapNaturalytelser(List<NaturalYtelse> naturalYtelser) {
        return naturalYtelser.stream()
            .map(n -> new OverstyrInntektsmeldingRequest.BortfaltNaturalytelseRequestDto(n.getPeriode().getFomDato(), n.getPeriode().getTomDato(), n.getType(), n.getBeloepPerMnd().getVerdi()))
            .toList();
    }

    private List<OverstyrInntektsmeldingRequest.RefusjonendringRequestDto> mapRefusjonsendringer(List<Refusjon> endringerRefusjon, LocalDate opphørFom) {
        // Endringer etter opphørsdato er ikke relevant
        var endringer = endringerRefusjon.stream()
            .filter(e -> e.getFom().isBefore(opphørFom))
            .map(e -> new OverstyrInntektsmeldingRequest.RefusjonendringRequestDto(e.getFom(), e.getRefusjonsbeløp().getVerdi()))
            .collect(Collectors.toList());
        // Setter opphør
        endringer.add(new OverstyrInntektsmeldingRequest.RefusjonendringRequestDto(opphørFom, BigDecimal.ZERO));
        return endringer;
    }

    public void lagForespørsel(String ag, BehandlingReferanse ref, Skjæringstidspunkt stp) {
        if (!OrganisasjonsNummerValidator.erGyldig(ag)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("FpInntektsmeldingTjeneste: Oppretter ikke forespørsel for saksnummer: {} fordi orgnummer: {} ikke er gyldig", ref.saksnummer(), tilMaskertNummer(ag));
            }
            return;
        }

        var request = new OpprettForespørselRequest(new OpprettForespørselRequest.AktørIdDto(ref.aktørId().getId()),
            new OrganisasjonsnummerDto(ag), stp.getUtledetSkjæringstidspunkt(), mapYtelsetype(ref.fagsakYtelseType()),
            new SaksnummerDto(ref.saksnummer().getVerdi()), stp.getFørsteUttaksdato());
        var opprettForespørselResponse = klient.opprettForespørsel(request);
        if (opprettForespørselResponse.forespørselResultat().equals(OpprettForespørselResponse.ForespørselResultat.FORESPØRSEL_OPPRETTET)) {
            lagHistorikkForForespørsel(ag, ref);
        } else {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Fpinntektsmelding har allerede en åpen oppgave på saksnummer: {} og orgnummer: {}", ref.saksnummer(), tilMaskertNummer(ag));
            }
        }
    }

    private void lagHistorikkForForespørsel(String ag, BehandlingReferanse ref) {
        var virksomhet = arbeidsgiverTjeneste.hentVirksomhet(ag);
        var agNavn = String.format("%s (%s)", virksomhet.getNavn(), virksomhet.getOrgnr());
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setBehandlingId(ref.behandlingId());
        historikkinnslag.setFagsakId(ref.fagsakId());
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(HistorikkinnslagType.MIN_SIDE_ARBEIDSGIVER);

        var beg = String.format("Oppgave til %s om å sende inntektsmelding", agNavn);
        new HistorikkInnslagTekstBuilder().medHendelse(HistorikkinnslagType.MIN_SIDE_ARBEIDSGIVER)
            .medBegrunnelse(beg)
            .build(historikkinnslag);
        historikkRepo.lagre(historikkinnslag);
    }

    private OpprettForespørselRequest.YtelseType mapYtelsetype(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case FORELDREPENGER -> OpprettForespørselRequest.YtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> OpprettForespørselRequest.YtelseType.SVANGERSKAPSPENGER;
            case UDEFINERT, ENGANGSTØNAD -> throw new IllegalArgumentException("Kan ikke opprette forespørsel for ytelsetype " + fagsakYtelseType);
        };
    }

    public void lagLukkForespørselTask(Behandling behandling, OrgNummer orgNummer, ForespørselStatus status) {
        var behandlingId = behandling.getId();
        var taskdata = ProsessTaskData.forTaskType(TaskType.forProsessTask(LukkForespørslerImTask.class));
        taskdata.setBehandling(behandling.getFagsakId(), behandlingId);
        taskdata.setCallIdFraEksisterende();
        if (orgNummer != null) {
            taskdata.setProperty(LukkForespørslerImTask.ORG_NUMMER, orgNummer.getId());
        }
        taskdata.setProperty(LukkForespørslerImTask.STATUS, status.name());
        taskdata.setProperty(LukkForespørslerImTask.SAK_NUMMER, behandling.getFagsak().getSaksnummer().getVerdi());
        prosessTaskTjeneste.lagre(taskdata);
    }

    public void lukkForespørsel(String saksnummer, String orgnummer) {
        if (!OrganisasjonsNummerValidator.erGyldig(orgnummer)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("FpInntektsmeldingTjeneste: Lukker ikke forespørsel for saksnummer: {} fordi orgnummer: {} ikke er gyldig", saksnummer, tilMaskertNummer(orgnummer));
            }
            return;
        }
        var request = new LukkForespørselRequest(new OrganisasjonsnummerDto(orgnummer), new SaksnummerDto(saksnummer));
        klient.lukkForespørsel(request);
    }

    public void settForespørselTilUtgått(String saksnummer) {
        var request = new LukkForespørselRequest(null, new SaksnummerDto(saksnummer));
        klient.settForespørselTilUtgått(request);
    }
}
