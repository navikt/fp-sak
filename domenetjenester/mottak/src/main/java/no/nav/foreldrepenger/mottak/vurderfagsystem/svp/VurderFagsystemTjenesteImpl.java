package no.nav.foreldrepenger.mottak.vurderfagsystem.svp;

import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING;
import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesUtils;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemTjeneste;

@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class VurderFagsystemTjenesteImpl implements VurderFagsystemTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(VurderFagsystemTjenesteImpl.class);

    private static final TemporalAmount seksMåneder = Period.parse("P6M");


    private BehandlingRepository behandlingRepository;
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private VurderFagsystemFellesUtils fellesUtils;

    public VurderFagsystemTjenesteImpl() {
        //Jaha jaha jaha
    }

    @Inject
    public VurderFagsystemTjenesteImpl(VurderFagsystemFellesUtils utils,
                                       BehandlingRepository behandlingRepository,
                                       SvangerskapspengerRepository svangerskapspengerRepository) {
        this.behandlingRepository = behandlingRepository;
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.fellesUtils = utils;
    }

    @Override
    public BehandlendeFagsystem vurderFagsystemStrukturertSøknad(VurderFagsystem vurderFagsystem, List<Fagsak> sakerGittYtelseType) {
        if (sakerGittYtelseType.isEmpty()) {
            return new BehandlendeFagsystem(VEDTAKSLØSNING);
        }

        var harSattHendelseDato = vurderFagsystem.getBarnTermindato().isPresent() || vurderFagsystem.getBarnFodselsdato().isPresent();
        if (!harSattHendelseDato) {
            return vurderElektroniskSøknadGammel(vurderFagsystem, sakerGittYtelseType);
        }

        var relevanteFagsaker = sakerGittYtelseType.stream()
            .filter(s -> fellesUtils.erFagsakPassendeForSøknadFamilieHendelse(vurderFagsystem, s, true))
            .toList();

        if (relevanteFagsaker.size() > 1) {
            var saksnumre = relevanteFagsaker.stream().map(Fagsak::getSaksnummer).toList();
            LOG.info("VurderFagsystem SV strukturert søknad {} flere relevante saker {}", vurderFagsystem.getJournalpostIdLog(), saksnumre);
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }
        if (relevanteFagsaker.isEmpty()) {
            return new BehandlendeFagsystem(VEDTAKSLØSNING);
        }
        // Har har vi kun 1 relevant sak. Sjekker om åpen eller basert på inntektsmelding og så henlagt før søknad kommer.
        var relevantFagsak = relevanteFagsaker.getFirst();
        if (relevantFagsak.erÅpen()) {
            return new BehandlendeFagsystem(VEDTAKSLØSNING, relevantFagsak.getSaksnummer());
        }
        if (fellesUtils.erFagsakBasertPåInntektsmeldingUtenSøknad(relevantFagsak)) {
            return fellesUtils.kanFagsakBasertPåInntektsmeldingBrukesForSøknad(vurderFagsystem, relevantFagsak) ?
                new BehandlendeFagsystem(VEDTAKSLØSNING, relevantFagsak.getSaksnummer()) : new BehandlendeFagsystem(VEDTAKSLØSNING);
        }
        LOG.info("VurderFagsystem SV strukturert søknad {} 1 relevant saker som ikke matchet {}", vurderFagsystem.getJournalpostIdLog(), relevantFagsak.getSaksnummer());
        return new BehandlendeFagsystem(MANUELL_VURDERING);
    }

    @Override
    public BehandlendeFagsystem vurderFagsystemInntektsmelding(VurderFagsystem vurderFagsystem, List<Fagsak> sakerGittYtelseType) {
        if (sakerGittYtelseType.isEmpty()) {
            return new BehandlendeFagsystem(VEDTAKSLØSNING);
        }

        var åpneFagsaker = fellesUtils.finnÅpneSaker(sakerGittYtelseType);
        if (åpneFagsaker.size() > 1) {
            var saksnumre = åpneFagsaker.stream().map(Fagsak::getSaksnummer).toList();
            LOG.info("VurderFagsystem SV IM {} flere åpne saker {}", vurderFagsystem.getJournalpostIdLog(), saksnumre);
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }
        if (åpneFagsaker.size() == 1) {
            return vurderFørstegangsbehandling(vurderFagsystem, åpneFagsaker.get(0));
        }

        var aktuelleSakerForMatch = sakerGittYtelseType.stream()
            .filter(f -> fellesUtils.finnGjeldendeFamilieHendelseSVP(f).map(this::hendelseDatoIPeriode).orElse(Boolean.TRUE))
            .toList();
        if (aktuelleSakerForMatch.size() > 1) {
            var saksnumre = aktuelleSakerForMatch.stream().map(Fagsak::getSaksnummer).toList();
            LOG.info("VurderFagsystem SV IM {} flere aktuelle saker {}", vurderFagsystem.getJournalpostIdLog(), saksnumre);
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }
        if (aktuelleSakerForMatch.size() == 1) {
            return vurderFørstegangsbehandling(vurderFagsystem, aktuelleSakerForMatch.get(0));
        }

        return new BehandlendeFagsystem(VEDTAKSLØSNING);
    }

    @Override
    public BehandlendeFagsystem vurderFagsystemUstrukturert(VurderFagsystem vurderFagsystem, List<Fagsak> sakerGittYtelseType) {
        if (VurderFagsystemFellesUtils.erSøknad(vurderFagsystem) && vurderFagsystem.getDokumentTypeId().erSøknadType() && sakerGittYtelseType.isEmpty()) {
            return new BehandlendeFagsystem(VEDTAKSLØSNING);
        }
        return fellesUtils.standardUstrukturertDokumentVurdering(sakerGittYtelseType).orElse(new BehandlendeFagsystem(MANUELL_VURDERING));
    }

    private BehandlendeFagsystem vurderElektroniskSøknadGammel(VurderFagsystem vurderFagsystem, List<Fagsak> sakerGittYtelseType) {
        if (sakerGittYtelseType.isEmpty()) {
            return new BehandlendeFagsystem(VEDTAKSLØSNING);
        }

        var åpneFagsaker = fellesUtils.finnÅpneSaker(sakerGittYtelseType);
        if (åpneFagsaker.size() > 1) {
            var saksnumre = åpneFagsaker.stream().map(Fagsak::getSaksnummer).toList();
            LOG.info("VurderFagsystem SV strukturert søknad gammel {} flere åpne saker {}", vurderFagsystem.getJournalpostIdLog(), saksnumre);
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }
        if (åpneFagsaker.size() == 1) {
            return vurderFørstegangsbehandling(vurderFagsystem, åpneFagsaker.get(0));
        }

        var aktuelleSakerForMatch = sakerGittYtelseType.stream()
            .filter(f -> fellesUtils.finnGjeldendeFamilieHendelseSVP(f).map(this::hendelseDatoIPeriode).orElse(Boolean.TRUE))
            .map(Fagsak::getSaksnummer)
            .toList();
        if (!aktuelleSakerForMatch.isEmpty()) {
            LOG.info("VurderFagsystem SV strukturert søknad gammel {} flere aktuelle saker {}", vurderFagsystem.getJournalpostIdLog(), aktuelleSakerForMatch);
        }
        return aktuelleSakerForMatch.isEmpty() ? new BehandlendeFagsystem(VEDTAKSLØSNING) : new BehandlendeFagsystem(MANUELL_VURDERING);
    }

    private BehandlendeFagsystem vurderFørstegangsbehandling(VurderFagsystem vurderFagsystem, Fagsak fagsak) {
        if (vurderFagsystem.erInntektsmelding() || fagsakManglerSøknad(fagsak)) {
            return new BehandlendeFagsystem(VEDTAKSLØSNING, fagsak.getSaksnummer());
        }
        if (fellesUtils.finnGjeldendeFamilieHendelseSVP(fagsak).map(this::hendelseDatoIPeriode).orElse(Boolean.TRUE)) {
            return new BehandlendeFagsystem(VEDTAKSLØSNING, fagsak.getSaksnummer());
        }
        LOG.info("VurderFagsystem SV strukturert søknad førstegang {} gir manuell {}", vurderFagsystem.getJournalpostIdLog(), fagsak.getSaksnummer());
        return new BehandlendeFagsystem(MANUELL_VURDERING);
    }

    private Boolean fagsakManglerSøknad(Fagsak fagsak) {
        return behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsak.getId()).map(behandling -> svangerskapspengerRepository.hentGrunnlag(behandling.getId()).isEmpty()).orElse(true);
    }

    private Boolean hendelseDatoIPeriode(FamilieHendelseEntitet familieHendelse) {
        var fødsel = familieHendelse.getFødselsdato();
        if (fødsel.isPresent() && fødsel.get().isAfter(LocalDate.now().minus(seksMåneder))) {
            return true;

        }
        var termin = familieHendelse.getTerminbekreftelse();
        if (termin.isPresent()) {
            var termindato = termin.get().getTermindato();
            return termindato.isAfter(LocalDate.now().minus(seksMåneder));
        }
        return false;
    }
}
