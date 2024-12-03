package no.nav.foreldrepenger.domene.registerinnhenting.impl;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.fraTilEquals;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;

@ApplicationScoped
public class RegisterinnhentingHistorikkinnslagTjeneste {

    private Historikkinnslag2Repository historikkinnslag2Repository;

    RegisterinnhentingHistorikkinnslagTjeneste() {
        // for CDI proxy
    }

    @Inject
    public RegisterinnhentingHistorikkinnslagTjeneste(Historikkinnslag2Repository historikkinnslag2Repository) {
        this.historikkinnslag2Repository = historikkinnslag2Repository;
    }

    public void opprettHistorikkinnslagForNyeRegisteropplysninger(Behandling behandling) {
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medTittel("Nye registeropplysninger")
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .addTekstlinje("Saksbehandling starter på nytt")
            .build();
        historikkinnslag2Repository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagForTilbakespoling(Behandling behandling, BehandlingStegType førSteg, BehandlingStegType etterSteg) {
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medTittel("Behandlingen er flyttet")
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .addTekstlinje("Behandlingen er flyttet fra " + førSteg.getNavn() + " tilbake til " + etterSteg.getNavn())
            .build();
        historikkinnslag2Repository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagForBehandlingMedNyeOpplysninger(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medTittel("Behandlingen oppdatert med nye opplysninger")
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .addTekstlinje(behandlingÅrsakType.getNavn())
            .build();
        historikkinnslag2Repository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagForEndretStartdatoEtterFødselshendelse(Behandling behandling, LocalDate endretFra, LocalDate endretTil) {
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medTittel(SkjermlenkeType.KONTROLL_AV_SAKSOPPLYSNINGER)
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .addTekstlinje(fraTilEquals("Startdato for foreldrepengeperioden", endretFra, endretTil))
            .build();
        historikkinnslag2Repository.lagre(historikkinnslag);
    }
}
