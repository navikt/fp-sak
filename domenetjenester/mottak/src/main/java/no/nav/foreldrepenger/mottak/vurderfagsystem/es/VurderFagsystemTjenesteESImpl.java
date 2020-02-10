package no.nav.foreldrepenger.mottak.vurderfagsystem.es;

import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING;
import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesUtils;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemTjeneste;

@FagsakYtelseTypeRef("ES")
@ApplicationScoped
public class VurderFagsystemTjenesteESImpl implements VurderFagsystemTjeneste {

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
        List<Fagsak> passendeFagsaker = sakerGittYtelseType.stream()
            .filter(s -> fellesUtils.erFagsakPassendeForFamilieHendelse(vurderFagsystem, s))
            .collect(Collectors.toList());

        if (passendeFagsaker.size() == 1) {
            return new BehandlendeFagsystem(VEDTAKSLØSNING).medSaksnummer(passendeFagsaker.get(0).getSaksnummer());
        } else if (passendeFagsaker.size() > 1) {
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

        Optional<BehandlendeFagsystem> standardVurdering = fellesUtils.standardUstrukturertDokumentVurdering(kompatibleFagsaker);
        if (standardVurdering.isPresent() || !VurderFagsystemFellesUtils.erSøknad(vurderFagsystem)) {
            return standardVurdering.orElse(new BehandlendeFagsystem(MANUELL_VURDERING));
        }

        if (fellesUtils.harSakOpprettetInnenIntervall(kompatibleFagsaker)) {
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }
        return new BehandlendeFagsystem(VEDTAKSLØSNING);
    }
}
