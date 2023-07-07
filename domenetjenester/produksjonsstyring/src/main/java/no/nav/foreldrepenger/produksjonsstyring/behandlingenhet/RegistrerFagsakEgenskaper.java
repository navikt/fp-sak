package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet;

import static java.time.temporal.ChronoUnit.DAYS;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
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

    private final BehandlendeEnhetTjeneste enhetTjeneste;
    private final PersoninfoAdapter personinfo;
    private final MedlemskapRepository medlemskapRepository;
    private final FagsakEgenskapRepository fagsakEgenskapRepository;
    private final InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    public RegistrerFagsakEgenskaper(BehandlendeEnhetTjeneste enhetTjeneste,
                                     PersoninfoAdapter personinfo,
                                     MedlemskapRepository medlemskapRepository,
                                     FagsakEgenskapRepository fagsakEgenskapRepository,
                                     InntektArbeidYtelseTjeneste iayTjeneste) {
        this.enhetTjeneste = enhetTjeneste;
        this.personinfo = personinfo;
        this.medlemskapRepository = medlemskapRepository;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
        this.iayTjeneste = iayTjeneste;
    }

    public FagsakMarkering fagsakEgenskaperForBruker(Behandling behandling) {
        var gjeldendeMarkering = fagsakEgenskapRepository.finnFagsakMarkering(behandling.getFagsakId());

        if (behandling.erManueltOpprettet() || gjeldendeMarkering.filter(FagsakMarkering::erPrioritert).isPresent()) {
            return gjeldendeMarkering.orElse(FagsakMarkering.NASJONAL);
        }

        // Har registrert en tilknytning og ikke utflyttet
        if (personinfo.hentGeografiskTilknytning(behandling.getAktørId()) != null) {
            return gjeldendeMarkering.orElse(FagsakMarkering.NASJONAL);
        }
        fagsakEgenskapRepository.lagreEgenskapUtenHistorikk(behandling.getFagsakId(), FagsakMarkering.BOSATT_UTLAND);
        enhetTjeneste.sjekkSkalOppdatereEnhet(behandling, FagsakMarkering.BOSATT_UTLAND)
            .ifPresent(e -> enhetTjeneste.oppdaterBehandlendeEnhet(behandling, e, HistorikkAktør.VEDTAKSLØSNINGEN, "Personopplysning"));
        return FagsakMarkering.BOSATT_UTLAND;
    }

    public FagsakMarkering fagsakEgenskaperFraSøknad(Behandling behandling, boolean oppgittRelasjonTilEØS) {
        var gjeldendeMarkering = fagsakEgenskapRepository.finnFagsakMarkering(behandling.getFagsakId());

        if (!BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType()) || gjeldendeMarkering.isPresent()) {
            return gjeldendeMarkering.orElse(FagsakMarkering.NASJONAL);
        }

        var saksmarkering = FagsakMarkering.NASJONAL;
        if (vurderOppgittUtlandsopphold(behandling.getId())) {
            saksmarkering = FagsakMarkering.BOSATT_UTLAND;
        } else if (oppgittRelasjonTilEØS) {
            saksmarkering = FagsakMarkering.EØS_BOSATT_NORGE;
        } else if (harOppgittEgenNæring(behandling.getId())) {
            saksmarkering = FagsakMarkering.SELVSTENDIG_NÆRING;
        }
        if (!FagsakMarkering.NASJONAL.equals(saksmarkering)) {
            fagsakEgenskapRepository.lagreEgenskapUtenHistorikk(behandling.getFagsakId(), saksmarkering);
            enhetTjeneste.sjekkSkalOppdatereEnhet(behandling, saksmarkering)
                .ifPresent(e -> enhetTjeneste.oppdaterBehandlendeEnhet(behandling, e, HistorikkAktør.VEDTAKSLØSNINGEN, "Søknadsopplysning"));
        }
        return saksmarkering;
    }

    public boolean harVurdertInnhentingDokumentasjon(Behandling behandling) {
        return fagsakEgenskapRepository.finnUtlandDokumentasjonStatus(behandling.getFagsakId()).isPresent();
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
            .flatMap(InntektArbeidYtelseGrunnlag::getOppgittOpptjening)
            .map(OppgittOpptjening::getEgenNæring).orElse(List.of())
            .isEmpty();
    }

}
