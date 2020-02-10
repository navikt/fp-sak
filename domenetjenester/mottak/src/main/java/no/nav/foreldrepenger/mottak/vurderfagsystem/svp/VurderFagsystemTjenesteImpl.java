package no.nav.foreldrepenger.mottak.vurderfagsystem.svp;

import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING;
import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesUtils;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemTjeneste;
import no.nav.vedtak.util.FPDateUtil;

@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class VurderFagsystemTjenesteImpl implements VurderFagsystemTjeneste {

    private static final TemporalAmount seksMåneder = Period.parse("P6M");


    private BehandlingRepository behandlingRepository;
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private VurderFagsystemFellesUtils fellesUtils;

    public VurderFagsystemTjenesteImpl() {
        //Jaha jaha jaha
    }

    @Inject
    public VurderFagsystemTjenesteImpl(VurderFagsystemFellesUtils utils,
                                       BehandlingRepositoryProvider repositoryProvider) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.svangerskapspengerRepository = repositoryProvider.getSvangerskapspengerRepository();
        this.fellesUtils = utils;
    }

    @Override
    public BehandlendeFagsystem vurderFagsystemStrukturertSøknad(VurderFagsystem vurderFagsystem, List<Fagsak> sakerGittYtelseType) {
        return vurderElektroniskDokument(vurderFagsystem, sakerGittYtelseType);
    }

    @Override
    public BehandlendeFagsystem vurderFagsystemInntektsmelding(VurderFagsystem vurderFagsystem, List<Fagsak> sakerGittYtelseType) {
        return vurderElektroniskDokument(vurderFagsystem, sakerGittYtelseType);
    }

    @Override
    public BehandlendeFagsystem vurderFagsystemUstrukturert(VurderFagsystem vurderFagsystem, List<Fagsak> sakerGittYtelseType) {
        return fellesUtils.standardUstrukturertDokumentVurdering(sakerGittYtelseType).orElse(new BehandlendeFagsystem(MANUELL_VURDERING));
    }

    private BehandlendeFagsystem vurderElektroniskDokument(VurderFagsystem vurderFagsystem, List<Fagsak> sakerGittYtelseType) {
        if (sakerGittYtelseType.isEmpty()) {
            return new BehandlendeFagsystem(VEDTAKSLØSNING);
        }

        List<Fagsak> åpneFagsaker = fellesUtils.finnÅpneSaker(sakerGittYtelseType);
        if (åpneFagsaker.size() > 1) {
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }
        if (åpneFagsaker.size() == 1) {
            return vurderFørstegangsbehandling(vurderFagsystem, åpneFagsaker.get(0));
        }

        List<Fagsak> aktuelleSakerForMatch = sakerGittYtelseType.stream()
            .filter(f -> fellesUtils.finnGjeldendeFamilieHendelse(f).map(this::hendelseDatoIPeriode).orElse(Boolean.TRUE))
            .collect(Collectors.toList());
        if (aktuelleSakerForMatch.size() > 1) {
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }
        if (aktuelleSakerForMatch.size() == 1) {
            return vurderFørstegangsbehandling(vurderFagsystem, aktuelleSakerForMatch.get(0));
        }

        return new BehandlendeFagsystem(VEDTAKSLØSNING);
    }

    private BehandlendeFagsystem vurderFørstegangsbehandling(VurderFagsystem vurderFagsystem, Fagsak fagsak) {
        if (vurderFagsystem.erInntektsmelding() || fagsakManglerSøknad(fagsak)) {
            return new BehandlendeFagsystem(VEDTAKSLØSNING).medSaksnummer(fagsak.getSaksnummer());
        }
        if (fellesUtils.finnGjeldendeFamilieHendelse(fagsak).map(this::hendelseDatoIPeriode).orElse(Boolean.TRUE)) {
            return new BehandlendeFagsystem(VEDTAKSLØSNING).medSaksnummer(fagsak.getSaksnummer());
        }
        return new BehandlendeFagsystem(MANUELL_VURDERING);
    }

    private Boolean fagsakManglerSøknad(Fagsak fagsak) {
        return behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).map(behandling -> svangerskapspengerRepository.hentGrunnlag(behandling.getId()).isEmpty()).orElse(true);
    }

    private Boolean hendelseDatoIPeriode(FamilieHendelseEntitet familieHendelse) {
        if (familieHendelse.getFødselsdato().isPresent()) {
            if (familieHendelse.getFødselsdato().get().isAfter(FPDateUtil.iDag().minus(seksMåneder))) {
                return true;
            }
        }
        if (familieHendelse.getTerminbekreftelse().isPresent()) {
            LocalDate termindato = familieHendelse.getTerminbekreftelse().get().getTermindato();
            return termindato.isAfter(FPDateUtil.iDag().minus(seksMåneder));
        }
        return false;
    }
}
