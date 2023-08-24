package no.nav.foreldrepenger.domene.registerinnhenting.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.*;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

import java.time.LocalDate;

@ApplicationScoped
public class RegisterinnhentingHistorikkinnslagTjeneste {

    private HistorikkRepository historikkRepository;

    RegisterinnhentingHistorikkinnslagTjeneste() {
        // for CDI proxy
    }

    @Inject
    public RegisterinnhentingHistorikkinnslagTjeneste(HistorikkRepository historikkRepository) {
        this.historikkRepository = historikkRepository;
    }

    public void opprettHistorikkinnslagForNyeRegisteropplysninger(Behandling behandling) {
        var nyeRegisteropplysningerInnslag = new Historikkinnslag();
        nyeRegisteropplysningerInnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        nyeRegisteropplysningerInnslag.setType(HistorikkinnslagType.NYE_REGOPPLYSNINGER);
        nyeRegisteropplysningerInnslag.setBehandlingId(behandling.getId());

        var historieBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.NYE_REGOPPLYSNINGER)
            .medBegrunnelse(HistorikkBegrunnelseType.SAKSBEH_START_PA_NYTT);
        historieBuilder.build(nyeRegisteropplysningerInnslag);
        historikkRepository.lagre(nyeRegisteropplysningerInnslag);
    }

    public void opprettHistorikkinnslagForTilbakespoling(Behandling behandling, BehandlingStegType førSteg, BehandlingStegType etterSteg) {
        var nyeRegisteropplysningerInnslag = new Historikkinnslag();
        nyeRegisteropplysningerInnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        nyeRegisteropplysningerInnslag.setType(HistorikkinnslagType.SPOLT_TILBAKE);
        nyeRegisteropplysningerInnslag.setBehandlingId(behandling.getId());

        var historieBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.SPOLT_TILBAKE)
            .medBegrunnelse("Behandlingen er flyttet fra " + førSteg.getNavn() + " tilbake til " + etterSteg.getNavn());
        historieBuilder.build(nyeRegisteropplysningerInnslag);
        historikkRepository.lagre(nyeRegisteropplysningerInnslag);
    }

    public void opprettHistorikkinnslagForBehandlingMedNyeOpplysninger(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        var nyeRegisteropplysningerInnslag = new Historikkinnslag();
        nyeRegisteropplysningerInnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        nyeRegisteropplysningerInnslag.setType(HistorikkinnslagType.BEH_OPPDATERT_NYE_OPPL);
        nyeRegisteropplysningerInnslag.setBehandlingId(behandling.getId());

        var historieBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.BEH_OPPDATERT_NYE_OPPL)
            .medBegrunnelse(behandlingÅrsakType);
        historieBuilder.build(nyeRegisteropplysningerInnslag);
        historikkRepository.lagre(nyeRegisteropplysningerInnslag);
    }

    public void opprettHistorikkinnslagForEndretStartdatoEtterFødselshendelse(Behandling behandling, LocalDate endretFra, LocalDate endretTil) {
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);

        var builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.FAKTA_ENDRET)
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_MEDLEMSKAP)
            .medEndretFelt(HistorikkEndretFeltType.STARTDATO_FRA_SOKNAD, endretFra, endretTil);

        builder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }
}
