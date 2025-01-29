package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet;

import static java.time.temporal.ChronoUnit.DAYS;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

@Dependent
public class RegistrerFagsakEgenskaper {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrerFagsakEgenskaper.class);

    private final BehandlendeEnhetTjeneste enhetTjeneste;
    private final PersoninfoAdapter personinfo;
    private final MedlemskapRepository medlemskapRepository;
    private final FagsakEgenskapRepository fagsakEgenskapRepository;
    private final InntektArbeidYtelseTjeneste iayTjeneste;
    private final YtelsesFordelingRepository ytelsesFordelingRepository;

    @Inject
    public RegistrerFagsakEgenskaper(BehandlendeEnhetTjeneste enhetTjeneste,
                                     PersoninfoAdapter personinfo,
                                     MedlemskapRepository medlemskapRepository,
                                     FagsakEgenskapRepository fagsakEgenskapRepository,
                                     InntektArbeidYtelseTjeneste iayTjeneste,
                                     YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.enhetTjeneste = enhetTjeneste;
        this.personinfo = personinfo;
        this.medlemskapRepository = medlemskapRepository;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
        this.iayTjeneste = iayTjeneste;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    public void fagsakEgenskaperForBruker(Behandling behandling) {
        var erMarkertMedPrioritet = fagsakEgenskapRepository.finnFagsakMarkeringer(behandling.getFagsakId()).stream()
            .anyMatch(FagsakMarkering::erPrioritert);

        if (behandling.erManueltOpprettet() || erMarkertMedPrioritet) {
            return;
        }

        var rutingResultat = enhetTjeneste.finnRuting(Set.of(behandling.getAktørId()));

        // Har registrert en tilknytning og ikke utflyttet
        if (personinfo.hentGeografiskTilknytning(behandling.getFagsakYtelseType(), behandling.getAktørId()) == null) {
            LOG.info("RUTING rfe {}", rutingResultat.contains(RutingResultat.UTLAND) ? "utland ok" : "diff utland fra hentGT");
            fagsakEgenskapRepository.leggTilFagsakMarkering(behandling.getFagsakId(), FagsakMarkering.BOSATT_UTLAND);
            BehandlendeEnhetTjeneste.sjekkSkalOppdatereEnhet(behandling, fagsakEgenskapRepository.finnFagsakMarkeringer(behandling.getFagsakId()))
                .ifPresent(e -> enhetTjeneste.oppdaterBehandlendeEnhet(behandling, e, HistorikkAktør.VEDTAKSLØSNINGEN, "Personopplysning"));
        } else {
            if (rutingResultat.contains(RutingResultat.UTLAND)) {
                LOG.info("RUTING rfe {}", "diff har utland fra tilgang");
            }
        }
    }

    public void fagsakEgenskaperFraSøknad(Behandling behandling, boolean oppgittRelasjonTilEØS) {
        var gjeldendeMarkering = fagsakEgenskapRepository.finnFagsakMarkeringer(behandling.getFagsakId());

        if (!BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType()) || !gjeldendeMarkering.isEmpty()) {
            return;
        }

        Set<FagsakMarkering> saksmarkering = new HashSet<>();
        if (vurderOppgittUtlandsopphold(behandling.getId())) {
            saksmarkering.add(FagsakMarkering.BOSATT_UTLAND);
        } else if (oppgittRelasjonTilEØS) {
            saksmarkering.add(FagsakMarkering.EØS_BOSATT_NORGE);
        }
        if (harOppgittEgenNæring(behandling.getId())) {
            saksmarkering.add(FagsakMarkering.SELVSTENDIG_NÆRING);
        }
        if (erBareFarRett(behandling)) {
            saksmarkering.add(FagsakMarkering.BARE_FAR_RETT);
        }
        if (!saksmarkering.isEmpty()) {
            saksmarkering.forEach(fsm -> fagsakEgenskapRepository.leggTilFagsakMarkering(behandling.getFagsakId(), fsm));
            BehandlendeEnhetTjeneste.sjekkSkalOppdatereEnhet(behandling, saksmarkering)
                .ifPresent(e -> enhetTjeneste.oppdaterBehandlendeEnhet(behandling, e, HistorikkAktør.VEDTAKSLØSNINGEN, "Søknadsopplysning"));
        }
    }

    public boolean harVurdertInnhentingDokumentasjon(Behandling behandling) {
        return fagsakEgenskapRepository.finnUtlandDokumentasjonStatus(behandling.getFagsakId()).isPresent();
    }

    public boolean erBareFarRett(Behandling behandling) {
        if (RelasjonsRolleType.MORA.equals(behandling.getRelasjonsRolleType())) {
            return false;
        }
        var yfAggregat = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandling.getId());
        if (yfAggregat.isEmpty()) {
            return false;
        }
        var aleneomsorg = yfAggregat.filter(YtelseFordelingAggregat::harAleneomsorg).isPresent();
        var bareFarRett = yfAggregat.filter(a -> a.harAnnenForelderRett(false)).isEmpty();
        return bareFarRett && !aleneomsorg;
    }

    private boolean vurderOppgittUtlandsopphold(Long behandlingId) {
        var oppgittUtlandsOpphold = medlemskapRepository.hentMedlemskap(behandlingId)
            .flatMap(MedlemskapAggregat::getOppgittTilknytning)
            .map(MedlemskapOppgittTilknytningEntitet::getOpphold).orElse(Set.of()).stream()
            .filter(opphold -> !Landkoder.NOR.equals(opphold.getLand()))
            .toList();
        var oppgittFramtidigUtlandsOpphold = oppgittUtlandsOpphold.stream().anyMatch(land -> !land.isTidligereOpphold());
        if (!oppgittFramtidigUtlandsOpphold) {
            return false;
        }
        var utlandSegmenter = oppgittUtlandsOpphold.stream()
            .map(opphold -> new LocalDateSegment<>(opphold.getPeriodeFom(), opphold.getPeriodeTom(), Boolean.TRUE))
            .collect(Collectors.toSet());
        return new LocalDateTimeline<>(utlandSegmenter, StandardCombinators::alwaysTrueForMatch).compress().stream()
            .anyMatch(segment -> Math.abs(DAYS.between(segment.getFom(), segment.getTom())) > 365);
    }

    private boolean harOppgittEgenNæring(Long behandlingId) {
        return !iayTjeneste.finnGrunnlag(behandlingId)
            .flatMap(InntektArbeidYtelseGrunnlag::getGjeldendeOppgittOpptjening)
            .map(OppgittOpptjening::getEgenNæring).orElse(List.of())
            .isEmpty();
    }

}
