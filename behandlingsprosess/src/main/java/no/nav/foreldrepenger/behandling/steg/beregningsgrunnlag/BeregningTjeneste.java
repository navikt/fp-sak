package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.oppdateringresultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

/**
 * Eksisterer for å kalle beregning i fpsak eller via rest til kalkulus
 */
@ApplicationScoped
public class BeregningTjeneste {

    private BeregningKalkulus kalkulusBeregner;
    private BeregningFPSAK fpsakBeregner;
    private boolean skalKalleKalkulus;

    public BeregningTjeneste() {
    }

    @Inject
    public BeregningTjeneste(BeregningKalkulus kalkulusBeregner, BeregningFPSAK fpsakBeregner) {
        this.kalkulusBeregner = kalkulusBeregner;
        this.fpsakBeregner = fpsakBeregner;
        this.skalKalleKalkulus = no.nav.foreldrepenger.konfig.Environment.current().isDev();
    }


    /**
     * Kjører beregning for angitt steg
     *
     * @param behandlingId       behandlingId
     * @param behandlingStegType Stegtype
     * @return Resultatstruktur med aksjonspunkter og eventuell vilkårsvurdering
     */
    public BeregningsgrunnlagVilkårOgAkjonspunktResultat beregn(Long behandlingId, BehandlingStegType behandlingStegType) {
        if (skalKalleKalkulus) {
            return kalkulusBeregner.beregn(behandlingId, behandlingStegType);
        } else {
            return fpsakBeregner.beregn(behandlingId, behandlingStegType);
        }
    }

    /**
     * Utfører overstyring
     *
     * @param behandlingReferanse                  behandlingId
     * @param overstyringAksjonspunktDto bekreftetAksjonspunktDto
     */
    public void overstyr(BehandlingReferanse behandlingReferanse, OverstyringAksjonspunktDto overstyringAksjonspunktDto) {
        if (skalKalleKalkulus) {
            kalkulusBeregner.overstyr(behandlingReferanse, overstyringAksjonspunktDto);
        } else {
            fpsakBeregner.overstyr(behandlingReferanse, overstyringAksjonspunktDto);
        }
    }

    /**
     * Lager historikk for overstyring
     *  @param behandlingReferanse                  behandlingId
     * @param overstyringAksjonspunktDto bekreftetAksjonspunktDto
     * @param tekstbuilder Tekstbuilder for historikk
     */
    public void lagOverstyrHistorikk(BehandlingReferanse behandlingReferanse,
                                     OverstyringAksjonspunktDto overstyringAksjonspunktDto,
                                     HistorikkInnslagTekstBuilder tekstbuilder) {
        if (skalKalleKalkulus) {
            kalkulusBeregner.lagOverstyringHistorikk(behandlingReferanse, overstyringAksjonspunktDto, tekstbuilder);
        } else {
            fpsakBeregner.lagOverstyringHistorikk(behandlingReferanse, overstyringAksjonspunktDto, tekstbuilder);
        }
    }



    /**
     * Gjør oppdatering etter bekreftelse av aksjonpunkt
     *
     * @param parameter                behandlingId
     * @param bekreftetAksjonspunktDto bekreftetAksjonspunktDto
     * @return resultat av oppdatering
     */
    public OppdaterBeregningsgrunnlagResultat oppdater(AksjonspunktOppdaterParameter parameter, BekreftetAksjonspunktDto bekreftetAksjonspunktDto) {
        if (skalKalleKalkulus) {
            return kalkulusBeregner.oppdater(parameter, bekreftetAksjonspunktDto);
        } else {
            return fpsakBeregner.oppdater(parameter, bekreftetAksjonspunktDto);
        }
    }


    /**
     * Henter beregningsgrunnlag
     *
     * @param behandlingId behandlingId
     * @return BeregningsgrunnlagGrunnlag
     */
    public Optional<BeregningsgrunnlagGrunnlag> hent(Long behandlingId) {
        if (skalKalleKalkulus) {
            return kalkulusBeregner.hent(behandlingId);
        } else {
            return fpsakBeregner.hent(behandlingId);
        }
    }

    public Optional<BeregningsgrunnlagDto> hentForGUI(Long behandlingId) {
        if (skalKalleKalkulus) {
            return kalkulusBeregner.hentForGUI(behandlingId);
        } else {
            return fpsakBeregner.hentForGUI(behandlingId);
        }
    }

    /**
     * Kopierer beregningsgrunnlag
     *
     * @param behandlingId behandlingId
     */
    public void kopier(Long behandlingId) {
        if (skalKalleKalkulus) {
            kalkulusBeregner.kopierFastsatt(behandlingId);
        } else {
            fpsakBeregner.kopierFastsatt(behandlingId);
        }
    }

    /**
     * Rydder beregningsgrunnlag og tilhørende resultat for et hopp bakover kall i oppgitt steg
     *
     * @param kontekst           Behandlingskontrollkontekst
     * @param behandlingStegType steget ryddkallet kjøres fra
     * @param tilSteg            Siste steg i hopp bakover transisjonen
     */
    public void rydd(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType, BehandlingStegType tilSteg) {

        if (skalKalleKalkulus) {
            kalkulusBeregner.rydd(kontekst, behandlingStegType, tilSteg);
        } else {
            fpsakBeregner.rydd(kontekst, behandlingStegType, tilSteg);
        }

    }

}
