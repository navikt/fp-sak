package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType.HENLAGT_FEILOPPRETTET;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningAksjonspunktDto;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarSaksopplysningerDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarSaksopplysningerOppdaterer implements AksjonspunktOppdaterer<AvklarSaksopplysningerDto> {

    private PersonopplysningTjeneste personopplysningTjeneste;

    private HistorikkTjenesteAdapter historikkAdapter;

    AvklarSaksopplysningerOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarSaksopplysningerOppdaterer(HistorikkTjenesteAdapter historikkAdapter, PersonopplysningTjeneste personopplysningTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarSaksopplysningerDto dto, AksjonspunktOppdaterParameter param) {
        Long behandlingId = param.getBehandlingId();
        AktørId aktørId = param.getAktørId();
        Behandling behandling = param.getBehandling();
        var ref = BehandlingReferanse.fra(behandling, param.getSkjæringstidspunkt());
        final PersonopplysningerAggregat personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(ref);
        boolean totrinn = håndterEndringHistorikk(dto, param, personopplysningerAggregat);

        if (dto.isFortsettBehandling()) {
            PersonopplysningAksjonspunktDto.PersonstatusPeriode personstatusPeriode =
                new PersonopplysningAksjonspunktDto.PersonstatusPeriode(dto.getPersonstatus(), personopplysningerAggregat.getPersonstatusFor(aktørId).getPeriode());
            personopplysningTjeneste.aksjonspunktAvklarSaksopplysninger(behandlingId, aktørId, new PersonopplysningAksjonspunktDto(personstatusPeriode));
            return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
        } else {
            return OppdateringResultat.medHenleggelse(HENLAGT_FEILOPPRETTET, dto.getBegrunnelse());
        }
    }

    private boolean håndterEndringHistorikk(AvklarSaksopplysningerDto dto, AksjonspunktOppdaterParameter param, PersonopplysningerAggregat personopplysningerAggregat) {
        if (dto.isFortsettBehandling()) {
            PersonstatusType bekreftetPersonstatus = PersonstatusType.fraKode(dto.getPersonstatus());
            PersonstatusType forrigePersonstatus = personopplysningerAggregat.getPersonstatusFor(param.getBehandling().getAktørId()).getPersonstatus();
            boolean endretVerdi = oppdaterVedEndretVerdi(HistorikkEndretFeltType.AVKLARSAKSOPPLYSNINGER, forrigePersonstatus, bekreftetPersonstatus);

            historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.BEHANDLING, null, HistorikkEndretFeltVerdiType.FORTSETT_BEHANDLING)
                .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
                .medSkjermlenke(SkjermlenkeType.KONTROLL_AV_SAKSOPPLYSNINGER);
            if (endretVerdi) {
                return true;
            }
        } else {
            historikkAdapter.tekstBuilder().medBegrunnelse(dto.getBegrunnelse())
                .medSkjermlenke(SkjermlenkeType.KONTROLL_AV_SAKSOPPLYSNINGER)
                .medEndretFelt(HistorikkEndretFeltType.BEHANDLING, null, HistorikkEndretFeltVerdiType.HENLEGG_BEHANDLING);
        }
        return false;
    }

    private boolean oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType, PersonstatusType original, PersonstatusType bekreftet) {
        if (!Objects.equals(bekreftet, original)) {
            historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, original, bekreftet);
            return true;
        }
        return false;
    }
}
