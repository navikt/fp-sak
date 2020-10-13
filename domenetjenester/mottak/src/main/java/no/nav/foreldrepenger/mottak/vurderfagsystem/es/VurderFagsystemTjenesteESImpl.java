package no.nav.foreldrepenger.mottak.vurderfagsystem.es;

import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING;
import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesUtils;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemTjeneste;

@FagsakYtelseTypeRef("ES")
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
        List<Fagsak> matchendeFagsaker = sakerGittYtelseType.stream()
            .filter(s -> fellesUtils.erFagsakMedFamilieHendelsePassendeForFamilieHendelse(vurderFagsystem, s))
            .collect(Collectors.toList());

        if (matchendeFagsaker.size() == 1) {
            return new BehandlendeFagsystem(VEDTAKSLØSNING).medSaksnummer(matchendeFagsaker.get(0).getSaksnummer());
        } else if (matchendeFagsaker.size() > 1) {
            LOG.info("VurderFagsystem ES strukturert søknad flere matchende saker {} for {}", matchendeFagsaker.size(), vurderFagsystem.getAktørId());
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }

        List<Fagsak> passendeFagsaker = sakerGittYtelseType.stream()
            .filter(s -> fellesUtils.erFagsakPassendeForFamilieHendelse(vurderFagsystem, s, false))
            .collect(Collectors.toList());

        if (passendeFagsaker.size() == 1) {
            return new BehandlendeFagsystem(VEDTAKSLØSNING).medSaksnummer(passendeFagsaker.get(0).getSaksnummer());
        } else if (passendeFagsaker.size() > 1) {
            LOG.info("VurderFagsystem ES strukturert søknad flere relevante saker {} for {}", passendeFagsaker.size(), vurderFagsystem.getAktørId());
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
        List<Fagsak> kompatibleFagsaker = fellesUtils.filtrerSakerForBehandlingTema(sakerGittYtelseType, vurderFagsystem.getBehandlingTema());

        if (VurderFagsystemFellesUtils.erSøknad(vurderFagsystem) && vurderFagsystem.getDokumentTypeId().erSøknadType() && kompatibleFagsaker.isEmpty()) {
            return new BehandlendeFagsystem(VEDTAKSLØSNING);
        }

        Optional<BehandlendeFagsystem> standardVurdering = fellesUtils.standardUstrukturertDokumentVurdering(kompatibleFagsaker);
        if (standardVurdering.isPresent() || !VurderFagsystemFellesUtils.erSøknad(vurderFagsystem)) {
            return standardVurdering.orElse(new BehandlendeFagsystem(MANUELL_VURDERING));
        }

        if (fellesUtils.harSakOpprettetInnenIntervall(kompatibleFagsaker)) {
            LOG.info("VurderFagsystem ES ustrukturert finnes nyere sak enn 10mnd for {}", vurderFagsystem.getAktørId());
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }
        return new BehandlendeFagsystem(VEDTAKSLØSNING);
    }
}
