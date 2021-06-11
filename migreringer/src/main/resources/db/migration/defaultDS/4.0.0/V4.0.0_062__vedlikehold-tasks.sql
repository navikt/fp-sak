update PROSESS_TASK_TYPE
set kode = 'generell.task.type.nummer1', navn = 'Generell task 1', beskrivelse = 'Generell task til div formål'
where kode = 'abakus.iaygrunnlag.migrering';

update PROSESS_TASK_TYPE
set kode = 'generell.task.type.nummer2', navn = 'Generell task 2', beskrivelse = 'Generell task til div formål'
where kode = 'abakus.iaygrunnlag.migrering.sammenligning';

update PROSESS_TASK_TYPE
set kode = 'generell.task.type.nummer3', navn = 'Generell task 3', beskrivelse = 'Generell task til div formål'
where kode = 'fordeling.opprettSakIGsak';

delete from PROSESS_TASK_TYPE
where kode in (
'abakus.vedtak.migrering',
'behandlingskontroll.gjenopptaOppdaterBehandling',
'behandlingskontroll.startBehandlingTomPapirsøknad',
'behandlingsstatus.fagsakRelasjonAvsluttningsdato',
'beregning.opprettGrunnbeløp',
'beregning.opprettRegisterAktiviteter',
'beregningsgrunnlag.opprettBeregningAktiviteter',
'dokumentbestiller.bestillDokument',
'fordeling.hentFraJoark',
'fordeling.hentOgVurderInfotrygdSak',
'fordeling.hentOgVurderVLSak',
'fordeling.klargjoering',
'fordeling.opprettSak',
'fordeling.opprettSakKobleFagsakOgGsak',
'fordeling.tilJournalforing',
'innhentsaksopplysninger.relaterteYtelser',
'integrasjon.gsak.opprettOppgave',
'iverksetteVedtak.varsleOmVedtak',
'oppdater.yf.soknad.mottattdato');
