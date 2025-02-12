package no.nav.foreldrepenger.domene.fpinntektsmelding;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.tilMaskertNummer;

import java.math.BigDecimal;
import java.time.Instant;
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
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrganisasjonsNummerValidator;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ApplicationScoped
public class FpInntektsmeldingTjeneste {
    private static final String GRUPPE_ID = "FPIM_TASK_%s";
    private FpinntektsmeldingKlient klient;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private HistorikkinnslagRepository historikkRepo;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste;

    private static final Logger LOG = LoggerFactory.getLogger(FpInntektsmeldingTjeneste.class);

    public FpInntektsmeldingTjeneste() {
        // CDI
    }

    @Inject
    public FpInntektsmeldingTjeneste(FpinntektsmeldingKlient klient,
                                     ProsessTaskTjeneste prosessTaskTjeneste,
                                     SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                     HistorikkinnslagRepository historikkRepo,
                                     ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                                     InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste) {
        this.klient = klient;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.historikkRepo = historikkRepo;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.inntektsmeldingRegisterTjeneste = inntektsmeldingRegisterTjeneste;
    }

    public void lagForespørselTask(BehandlingReferanse ref) {
        var taskdata = ProsessTaskData.forTaskType(TaskType.forProsessTask(FpinntektsmeldingTask.class));
        taskdata.setBehandling(ref.saksnummer().getVerdi(), ref.fagsakId(), ref.behandlingId());
        var gruppeId = String.format(GRUPPE_ID, ref.saksnummer().getVerdi());
        taskdata.setGruppe(gruppeId);
        taskdata.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
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

    public void lagForespørsel(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var arbeidsgivereViManglerInntektsmeldingFra = inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(ref, stp)
            .keySet()
            .stream()
            .filter(arbeidsgiver -> OrganisasjonsNummerValidator.erGyldig(arbeidsgiver.getOrgnr()))
            .map(arbeidsgiver -> new OrganisasjonsnummerDto(arbeidsgiver.getOrgnr()))
            .toList();
        if (arbeidsgivereViManglerInntektsmeldingFra.isEmpty()) {
            LOG.info("FpInntektsmeldingTjeneste:lagForespørsel: Ingen inntektsmeldinger mangler for sak {} og behandlingId {}", ref.saksnummer(), ref.behandlingId());
            return;
        }
        var skjæringstidspunkt = stp.getUtledetSkjæringstidspunkt();
        var førsteUttaksdato = stp.getFørsteUttaksdato();

        var request = new OpprettForespørselRequest(new OpprettForespørselRequest.AktørIdDto(ref.aktørId().getId()),
            null, skjæringstidspunkt, mapYtelsetype(ref.fagsakYtelseType()),
            new SaksnummerDto(ref.saksnummer().getVerdi()), førsteUttaksdato, arbeidsgivereViManglerInntektsmeldingFra);

        LOG.info("Sender kall til fpinntektsmelding om å opprette forespørsel for saksnummer {} med skjæringstidspunkt {} for følgende organisasjonsnumre: {}",  ref.saksnummer(), stp, arbeidsgivereViManglerInntektsmeldingFra);
        var opprettForespørselResponseNy = klient.opprettForespørsel(request);

        opprettForespørselResponseNy.organisasjonsnumreMedStatus().forEach( organisasjonsnummerMedStatus -> {
            var orgnr = organisasjonsnummerMedStatus.organisasjonsnummerDto().orgnr();
            if (organisasjonsnummerMedStatus.status().equals(OpprettForespørselResponsNy.ForespørselResultat.FORESPØRSEL_OPPRETTET)) {
                lagHistorikkForForespørsel(ref, String.format("Oppgave om å sende inntektsmelding er opprettet for %s.", hentArbeidsgivernavn(orgnr)));
            }else {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Fpinntektsmelding har allerede oppgave på saksnummer: {} og orgnummer: {} på stp: {} og første uttaksdato: {}",
                        ref.saksnummer(), tilMaskertNummer(orgnr), skjæringstidspunkt, førsteUttaksdato );
                }
            }
        });
    }

    private void lagHistorikkForForespørsel(BehandlingReferanse ref, String tekst) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medTittel("Min side - arbeidsgiver")
            .medBehandlingId(ref.behandlingId())
            .medFagsakId(ref.fagsakId())
            .addLinje(tekst)
            .build();

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
        taskdata.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandlingId);
        if (orgNummer != null) {
            taskdata.setProperty(LukkForespørslerImTask.ORG_NUMMER, orgNummer.getId());
        }
        var gruppeId = String.format(GRUPPE_ID, behandling.getSaksnummer().getVerdi());
        taskdata.setGruppe(gruppeId);
        taskdata.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
        taskdata.setProperty(LukkForespørslerImTask.STATUS, status.name());
        taskdata.setProperty(LukkForespørslerImTask.SAK_NUMMER, behandling.getSaksnummer().getVerdi());
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

    public SendNyBeskjedResponse sendNyBeskjedTilArbeidsgiver(BehandlingReferanse ref, String orgnummer) {
        var request = new NyBeskjedRequest(new OrganisasjonsnummerDto(orgnummer), new SaksnummerDto(ref.saksnummer().getVerdi()));
        return klient.sendNyBeskjedPåForespørsel(request);
    }

    private String hentArbeidsgivernavn(String ag) {
        var virksomhet = arbeidsgiverTjeneste.hentVirksomhet(ag);
        return String.format("%s (%s)", virksomhet.getNavn(), virksomhet.getOrgnr());
    }
}
