package no.nav.foreldrepenger.web.app.tjenester.hendelser.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;

import no.nav.foreldrepenger.kontrakter.abonnent.infotrygd.InfotrygdHendelseDto;
import no.nav.foreldrepenger.mottak.hendelser.MottattHendelseTjeneste;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.YtelseHendelse;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.EnkelRespons;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.ForretningshendelseRegistrerer;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.HendelserRestTjeneste;
import no.nav.vedtak.log.sporingslogg.Sporingsdata;
import no.nav.vedtak.log.sporingslogg.SporingsloggHelper;
import no.nav.vedtak.log.sporingslogg.StandardSporingsloggId;

@ApplicationScoped
@HendelseTypeRef(HendelseTypeRef.INFOTRYGD)
public class InfotrygdHendelseRegistrerer implements ForretningshendelseRegistrerer<InfotrygdHendelseDto> {

    private static final String CREATE_ACTION = "create";

    private MottattHendelseTjeneste mottattHendelseTjeneste;

    InfotrygdHendelseRegistrerer() {
        //CDI
    }

    @Inject
    public InfotrygdHendelseRegistrerer(MottattHendelseTjeneste mottattHendelseTjeneste) {
        this.mottattHendelseTjeneste = mottattHendelseTjeneste;
    }

    private static void loggSporingsdata(InfotrygdHendelseDto dto) {
        String endepunkt = HendelserRestTjeneste.class.getAnnotation(Path.class).value() + "/hendelse";
        Sporingsdata sd = Sporingsdata.opprett(endepunkt).leggTilId(StandardSporingsloggId.AKTOR_ID, dto.getAktørId());
        SporingsloggHelper.logSporing(HendelserRestTjeneste.class, sd, CREATE_ACTION, endepunkt);
    }

    @Override
    public EnkelRespons registrer(InfotrygdHendelseDto hendelseDto) {
        YtelseHendelse hendelse = new YtelseHendelse(hendelseDto.getHendelsetype(), hendelseDto.getTypeYtelse(),
            hendelseDto.getAktørId(), hendelseDto.getFom(), hendelseDto.getIdentdato());
        mottattHendelseTjeneste.registrerHendelse(hendelseDto.getId(), hendelse);
        loggSporingsdata(hendelseDto);
        return new EnkelRespons("OK");
    }
}
