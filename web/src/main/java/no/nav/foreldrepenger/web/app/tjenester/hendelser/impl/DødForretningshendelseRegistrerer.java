package no.nav.foreldrepenger.web.app.tjenester.hendelser.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.kontrakter.abonnent.tps.DødHendelseDto;
import no.nav.foreldrepenger.mottak.hendelser.MottattHendelseTjeneste;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.DødHendelse;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.EnkelRespons;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.ForretningshendelseRegistrerer;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.HendelserRestTjeneste;
import no.nav.vedtak.log.sporingslogg.Sporingsdata;
import no.nav.vedtak.log.sporingslogg.SporingsloggHelper;
import no.nav.vedtak.log.sporingslogg.StandardSporingsloggId;

@ApplicationScoped
@HendelseTypeRef(HendelseTypeRef.DØD)
public class DødForretningshendelseRegistrerer implements ForretningshendelseRegistrerer<DødHendelseDto> {

    private static final String CREATE_ACTION = "create";

    private MottattHendelseTjeneste mottattHendelseTjeneste;

    DødForretningshendelseRegistrerer() {
        //CDI
    }

    @Inject
    public DødForretningshendelseRegistrerer(MottattHendelseTjeneste mottattHendelseTjeneste) {
        this.mottattHendelseTjeneste = mottattHendelseTjeneste;
    }

    @Override
    public EnkelRespons registrer(DødHendelseDto hendelseDto) {
        List<String> aktørIdListe = hendelseDto.getAktørId().stream().map(AktørId::new).map(AktørId::getId).collect(Collectors.toList());
        DødHendelse hendelse = new DødHendelse(aktørIdListe, hendelseDto.getDødsdato());
        mottattHendelseTjeneste.registrerHendelse(hendelseDto.getId(), hendelse);
        loggSporingsdata(hendelseDto);
        return new EnkelRespons("OK");
    }

    private static void loggSporingsdata(DødHendelseDto dto) {
        String endepunkt = HendelserRestTjeneste.class.getAnnotation(Path.class).value() + "/dod";
        dto.getAktørId().stream().forEach(aktørId -> {
            Sporingsdata sd = Sporingsdata.opprett(endepunkt).leggTilId(StandardSporingsloggId.AKTOR_ID, aktørId);
            SporingsloggHelper.logSporing(HendelserRestTjeneste.class, sd, CREATE_ACTION, endepunkt);
        });
    }
}
