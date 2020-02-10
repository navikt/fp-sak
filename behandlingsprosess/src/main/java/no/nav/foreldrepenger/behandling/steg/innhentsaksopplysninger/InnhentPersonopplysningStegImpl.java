package no.nav.foreldrepenger.behandling.steg.innhentsaksopplysninger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.domene.registerinnhenting.RegisterdataInnhenter;
import no.nav.foreldrepenger.domene.registerinnhenting.impl.SaksopplysningerFeil;
import no.nav.foreldrepenger.domene.typer.AktørId;

@BehandlingStegRef(kode = "INPER")
@BehandlingTypeRef("BT-006") // Innsyn
@FagsakYtelseTypeRef
@ApplicationScoped
public class InnhentPersonopplysningStegImpl implements InnhentRegisteropplysningerSteg {

    private RegisterdataInnhenter registerdataInnhenter;

    InnhentPersonopplysningStegImpl() {
        // for CDI proxy
    }

    @Inject
    public InnhentPersonopplysningStegImpl(RegisterdataInnhenter registerdataInnhenter) {
        this.registerdataInnhenter = registerdataInnhenter;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        // TODO (essv): Avklare om vi må hente inn mer info om søker (medsøker, barn, +++)
        Personinfo søkerInfo = registerdataInnhenter.innhentSaksopplysningerForSøker(kontekst.getAktørId());
        validerSøkerinfo(kontekst.getAktørId(), søkerInfo);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void validerSøkerinfo(AktørId aktørId, Personinfo søkerInfo) {
        if (søkerInfo == null) {
            throw SaksopplysningerFeil.FACTORY.feilVedOppslagITPS(aktørId.getId()).toException();
        }
    }

}
