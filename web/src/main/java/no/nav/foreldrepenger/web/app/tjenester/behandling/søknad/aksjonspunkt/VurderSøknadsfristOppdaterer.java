package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp.SøknadsfristTjeneste;
import no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp.VurderSøknadsfristAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderSøknadsfristDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderSøknadsfristOppdaterer implements AksjonspunktOppdaterer<VurderSøknadsfristDto> {

    private SøknadsfristTjeneste søknadsfristTjeneste;
    private HistorikkTjenesteAdapter historikkAdapter;
    private BehandlingRepositoryProvider repositoryProvider;


    public VurderSøknadsfristOppdaterer() {
        // for CDI proxy
    }


    @Inject
    public VurderSøknadsfristOppdaterer(SøknadsfristTjeneste søknadsfristTjeneste,
                                        HistorikkTjenesteAdapter historikkAdapter, BehandlingRepositoryProvider repositoryProvider) {
        this.søknadsfristTjeneste = søknadsfristTjeneste;
        this.historikkAdapter = historikkAdapter;
        this.repositoryProvider = repositoryProvider;
    }

    @Override
    public OppdateringResultat oppdater(VurderSøknadsfristDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        SøknadEntitet søknad = repositoryProvider.getSøknadRepository().hentSøknad(behandling);
        LocalDate søknadensMottatDato = søknad.getMottattDato();

        opprettHistorikkinnslag(dto, param, søknad);

        VurderSøknadsfristAksjonspunktDto adapter = new VurderSøknadsfristAksjonspunktDto(
            dto.harGyldigGrunn() ? dto.getAnsesMottattDato() : søknadensMottatDato,
            dto.getBegrunnelse());
        søknadsfristTjeneste.lagreVurderSøknadsfristResultat(behandling, adapter);
        return OppdateringResultat.utenOveropp();
    }

    private void opprettHistorikkinnslag(VurderSøknadsfristDto dto, AksjonspunktOppdaterParameter param, SøknadEntitet søknad) {

        HistorikkInnslagTekstBuilder tekstBuilder = historikkAdapter.tekstBuilder()
            .medSkjermlenke(SkjermlenkeType.SOEKNADSFRIST)
            .medEndretFelt(HistorikkEndretFeltType.SOKNADSFRIST, null,
                dto.harGyldigGrunn() ? HistorikkEndretFeltVerdiType.HAR_GYLDIG_GRUNN : HistorikkEndretFeltVerdiType.HAR_IKKE_GYLDIG_GRUNN)
            .medBegrunnelse(dto.getBegrunnelse(),param.erBegrunnelseEndret());

        if (dto.harGyldigGrunn()) {
            Uttaksperiodegrense uttaksperiodegrense = repositoryProvider.getUttaksperiodegrenseRepository().hent(param.getBehandlingId());
            LocalDate lagretMottattDato = uttaksperiodegrense.getMottattDato();
            LocalDate tidligereAnseesMottattDato = søknad.getMottattDato().equals(lagretMottattDato) ? null : lagretMottattDato;
            LocalDate dtoMottattDato = dto.getAnsesMottattDato();

            if (!dtoMottattDato.equals(tidligereAnseesMottattDato)) {
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.MOTTATT_DATO, tidligereAnseesMottattDato, dtoMottattDato);
            }
        }
    }
}
