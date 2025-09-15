package no.nav.foreldrepenger.mottak.vurderfagsystem.es;

import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING;
import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesUtils;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemTjeneste;

@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
@ApplicationScoped
public class VurderFagsystemTjenesteESImpl implements VurderFagsystemTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(VurderFagsystemTjenesteESImpl.class);

    private VurderFagsystemFellesUtils fellesUtils;

    public VurderFagsystemTjenesteESImpl() {
        //For CDI|
    }

    @Inject
    public VurderFagsystemTjenesteESImpl(VurderFagsystemFellesUtils utils) {
        this.fellesUtils = utils;
    }

    @Override
    public BehandlendeFagsystem vurderFagsystemStrukturertSøknad(VurderFagsystem vurderFagsystem, List<Fagsak> sakerGittYtelseType) {
        var matchendeFagsaker = sakerGittYtelseType.stream()
            .filter(s -> fellesUtils.erFagsakMedFamilieHendelsePassendeForSøknadFamilieHendelse(vurderFagsystem, s))
            .map(Fagsak::getSaksnummer)
            .toList();

        if (matchendeFagsaker.size() == 1) {
            return new BehandlendeFagsystem(VEDTAKSLØSNING, matchendeFagsaker.getFirst());
        }
        if (matchendeFagsaker.size() > 1) {
            LOG.info("VurderFagsystem ES strukturert søknad {} flere matchende saker {}", vurderFagsystem.getJournalpostIdLog(), matchendeFagsaker);
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }

        var passendeFagsaker = sakerGittYtelseType.stream()
            .filter(s -> fellesUtils.erFagsakPassendeForSøknadFamilieHendelse(vurderFagsystem, s, false))
            .map(Fagsak::getSaksnummer)
            .toList();

        if (passendeFagsaker.size() == 1) {
            return new BehandlendeFagsystem(VEDTAKSLØSNING, passendeFagsaker.getFirst());
        }
        if (passendeFagsaker.size() > 1) {
            LOG.info("VurderFagsystem ES strukturert søknad {} flere relevante saker {}", vurderFagsystem.getJournalpostIdLog(), passendeFagsaker);
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }

        return new BehandlendeFagsystem(VEDTAKSLØSNING);
    }

    @Override
    public BehandlendeFagsystem vurderFagsystemInntektsmelding(VurderFagsystem vurderFagsystem, List<Fagsak> sakerGittYtelseType) {
        // Skal ikke kune skje
        return new BehandlendeFagsystem(MANUELL_VURDERING);
    }

    @Override
    public BehandlendeFagsystem vurderFagsystemUstrukturert(VurderFagsystem vurderFagsystem, List<Fagsak> sakerGittYtelseType) {
        var kompatibleFagsaker = fellesUtils.filtrerSakerForBehandlingTema(sakerGittYtelseType, vurderFagsystem.getBehandlingTema());

        if (VurderFagsystemFellesUtils.erSøknad(vurderFagsystem) && vurderFagsystem.getDokumentTypeId().erSøknadType() && kompatibleFagsaker.isEmpty()) {
            return new BehandlendeFagsystem(VEDTAKSLØSNING);
        }

        var standardVurdering = fellesUtils.standardUstrukturertDokumentVurdering(kompatibleFagsaker);
        if (standardVurdering.isPresent() || !VurderFagsystemFellesUtils.erSøknad(vurderFagsystem)) {
            return standardVurdering.orElse(new BehandlendeFagsystem(MANUELL_VURDERING));
        }

        var sakOpprettetInnenIntervall = fellesUtils.sakerOpprettetInnenIntervall(kompatibleFagsaker).stream()
            .map(Fagsak::getSaksnummer)
            .toList();
        if (!sakOpprettetInnenIntervall.isEmpty()) {
            LOG.info("VurderFagsystem ES ustrukturert {} finnes nyere sak enn 10mnd {}", vurderFagsystem.getJournalpostIdLog(), sakOpprettetInnenIntervall);
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }
        return new BehandlendeFagsystem(VEDTAKSLØSNING);
    }
}
