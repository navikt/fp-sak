package no.nav.foreldrepenger.mottak.vurderfagsystem.fp;

import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING;
import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING;
import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.VURDER_INFOTRYGD;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesUtils;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemTjeneste;

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class VurderFagsystemTjenesteImpl implements VurderFagsystemTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(VurderFagsystemTjenesteImpl.class);

    private FagsakRepository fagsakRepository;
    private VurderFagsystemFellesUtils fellesUtils;

    public VurderFagsystemTjenesteImpl() {
        //For CDI
    }

    @Inject
    public VurderFagsystemTjenesteImpl(VurderFagsystemFellesUtils utils,
                                         BehandlingRepositoryProvider repositoryProvider) {
        this.fellesUtils = utils;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
    }

    @Override
    public BehandlendeFagsystem vurderFagsystemStrukturertSøknad(VurderFagsystem vurderFagsystem, List<Fagsak> sakerGittYtelseType) {
        // NB: Man ønsker er å rute søknad inn på mulig sak og unngå unødvendig saksopretting. Mottak skal håndtere tilfellene
        List<Fagsak> matchendeFagsaker = sakerGittYtelseType.stream()
            .filter(s -> fellesUtils.erFagsakMedFamilieHendelsePassendeForFamilieHendelse(vurderFagsystem, s))
            .collect(Collectors.toList());

        if (matchendeFagsaker.size() == 1) {
            return new BehandlendeFagsystem(VEDTAKSLØSNING).medSaksnummer(matchendeFagsaker.get(0).getSaksnummer());
        } else if (matchendeFagsaker.size() > 1) {
            LOG.info("VurderFagsystem FP strukturert søknad flere matchende saker {} for {}", matchendeFagsaker.size(), vurderFagsystem.getAktørId());
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }

        List<Fagsak> relevanteFagsaker = sakerGittYtelseType.stream()
            .filter(s -> fellesUtils.erFagsakPassendeForFamilieHendelse(vurderFagsystem, s))
            .collect(Collectors.toList());

        if (relevanteFagsaker.size() == 1) {
            return new BehandlendeFagsystem(VEDTAKSLØSNING).medSaksnummer(relevanteFagsaker.get(0).getSaksnummer());
        } else if (relevanteFagsaker.size() > 1) {
            LOG.info("VurderFagsystem FP strukturert søknad flere relevante saker {} for {}", relevanteFagsaker.size(), vurderFagsystem.getAktørId());
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }

        if (fellesUtils.finnÅpneSaker(sakerGittYtelseType).size() > 1) {
            LOG.info("VurderFagsystem FP strukturert søknad mer enn 1 åpen sak for {}", vurderFagsystem.getAktørId());
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }
        if (fellesUtils.harSakOpprettetInnenIntervall(sakerGittYtelseType)) {
            LOG.info("VurderFagsystem FP strukturert søknad nyere sak enn 10mnd for {}", vurderFagsystem.getAktørId());
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }

        if (skalVurdereInfotrygdForAnnenPart(vurderFagsystem)) {
            return new BehandlendeFagsystem(VURDER_INFOTRYGD);
        } else {
            return new BehandlendeFagsystem(VEDTAKSLØSNING);
        }
    }

    @Override
    public BehandlendeFagsystem vurderFagsystemInntektsmelding(VurderFagsystem vurderFagsystem, List<Fagsak> sakerGittYtelseType) {
        // NB: Man ønsker er å rute søknad inn på mulig sak og unngå unødvendig saksopretting. Mottak skal håndtere tilfellene
        return fellesUtils.vurderAktuelleFagsakerForInntektsmeldingFP(vurderFagsystem, sakerGittYtelseType);
    }

    @Override
    public BehandlendeFagsystem vurderFagsystemUstrukturert(VurderFagsystem vurderFagsystem, List<Fagsak> sakerGittYtelseType) {
        List<Fagsak> kompatibleFagsaker = fellesUtils.filtrerSakerForBehandlingTema(sakerGittYtelseType, vurderFagsystem.getBehandlingTema());

        if (VurderFagsystemFellesUtils.erSøknad(vurderFagsystem) && (DokumentTypeId.UDEFINERT.equals(vurderFagsystem.getDokumentTypeId()) || !vurderFagsystem.getDokumentTypeId().erEndringsSøknadType())) {
            // Inntil videre kan man ikke se periode. OBS på forskjell mot ES: FP-saker lever mye lenger.
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }

        var vurdering = fellesUtils.standardUstrukturertDokumentVurdering(kompatibleFagsaker).orElse(new BehandlendeFagsystem(MANUELL_VURDERING));
        if (MANUELL_VURDERING.equals(vurdering.getBehandlendeSystem())) {
            LOG.info("VurderFagsystem FP ustrukturert vurdert til manuell behandling for {}", vurderFagsystem.getAktørId());
        }
        return vurdering;
    }

    private boolean harAnnenPartSakVL(VurderFagsystem vurderFagsystem) {
        return vurderFagsystem.getAnnenPart().map(fagsakRepository::hentForBruker).orElse(Collections.emptyList()).stream()
            .anyMatch(s -> fellesUtils.erFagsakPassendeForFamilieHendelse(vurderFagsystem, s));
    }

    private boolean skalVurdereInfotrygdForAnnenPart(VurderFagsystem vurderFagsystem) {
        if (vurderFagsystem.getAnnenPart().isEmpty()) {
            return false;
        }
        return !harAnnenPartSakVL(vurderFagsystem);
    }
}
