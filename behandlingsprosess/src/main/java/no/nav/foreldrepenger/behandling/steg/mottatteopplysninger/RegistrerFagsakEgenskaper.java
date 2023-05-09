package no.nav.foreldrepenger.behandling.steg.mottatteopplysninger;

import static java.time.temporal.ChronoUnit.DAYS;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
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

    private final PersoninfoAdapter personinfo;
    private final MedlemskapRepository medlemskapRepository;
    private final FagsakEgenskapRepository fagsakEgenskapRepository;
    private final InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    public RegistrerFagsakEgenskaper(PersoninfoAdapter personinfo, MedlemskapRepository medlemskapRepository,
                                     FagsakEgenskapRepository fagsakEgenskapRepository, InntektArbeidYtelseTjeneste iayTjeneste) {
        this.personinfo = personinfo;
        this.medlemskapRepository = medlemskapRepository;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
        this.iayTjeneste = iayTjeneste;
    }

    public FagsakMarkering registrerFagsakEgenskaper(Behandling behandling, boolean oppgittRelasjonTilEØS) {
        if (!BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType()) ||
            fagsakEgenskapRepository.finnFagsakMarkering(behandling.getFagsakId()).isPresent()) {
            return fagsakEgenskapRepository.finnFagsakMarkering(behandling.getFagsakId()).orElse(FagsakMarkering.NASJONAL);
        }
        var geografiskTilknyttetUtlandEllerUkjent = personinfo.hentGeografiskTilknytning(behandling.getAktørId()) == null;
        var medlemskapOppgittEttÅrUtlandsopphold = vurderOppgittUtlandsopphold(behandling.getId());

        var utlandMarkering = FagsakMarkering.NASJONAL;
        if (geografiskTilknyttetUtlandEllerUkjent || medlemskapOppgittEttÅrUtlandsopphold) {
            utlandMarkering = FagsakMarkering.BOSATT_UTLAND;
        } else if (oppgittRelasjonTilEØS) {
            utlandMarkering = FagsakMarkering.EØS_BOSATT_NORGE;
        } else if (harOppgittEgenNæring(behandling.getId())) {
            utlandMarkering = FagsakMarkering.SELVSTENDIG_NÆRING;
        }
        if (!FagsakMarkering.NASJONAL.equals(utlandMarkering)) {
            fagsakEgenskapRepository.lagreEgenskapUtenHistorikk(behandling.getFagsakId(), utlandMarkering);
        }
        return utlandMarkering;
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
