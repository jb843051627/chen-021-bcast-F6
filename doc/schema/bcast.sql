-- bcast 广播电视安全播出与播出事故处置管理 -- schema (chen-021)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_021

CREATE TABLE IF NOT EXISTS t_bcast_case_flow (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '播出事故处置单代号',
  stage int DEFAULT NULL COMMENT '当前步次 0..3（发现核实/处置抢险/恢复确认/上报结案）',
  status int DEFAULT NULL COMMENT '处置会落 0未起 1在办 2已收了口',
  content varchar(2000) DEFAULT NULL COMMENT '一段一记（挪步出口落下的分段记载：第k步|时刻|话；沉底的、收口的也留在这）',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次挪步动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='播出事故处置单';

CREATE TABLE IF NOT EXISTS t_bcast_off_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '运行情况报送册批号',
  row_no int DEFAULT NULL COMMENT '原报行次',
  item_code varchar(64) DEFAULT NULL COMMENT '被对上的监测台代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行停播分钟数',
  hours_num decimal(12,2) DEFAULT NULL COMMENT '本季满播小时数',
  status int DEFAULT NULL COMMENT '行落地情形 0未销 1已收下 2已退回',
  arc_flag int DEFAULT '0' COMMENT '挪出标记 0现行 1已挪出(连着两个报送季未销，挪出现行名册；不是抹掉，老册照翻)',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='运行情况报送核收行';

CREATE TABLE IF NOT EXISTS t_bcast_pre_line (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '定档门槛线代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '线名',
  line_kind int DEFAULT '0' COMMENT '管哪一头 0本段停播分钟线 1被影响户数线（旧卷这一栏空着的按分钟线认，守住存量）',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '起算数值',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '过半数值',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '封顶数值(分钟线按分钟，户数线按户)',
  eff_start datetime DEFAULT NULL COMMENT '立线之日',
  eff_end datetime DEFAULT NULL COMMENT '让位之日(不含)',
  priority int DEFAULT NULL COMMENT '让位顺位(数值大的先说话)',
  status int DEFAULT NULL COMMENT '线的情形 0在场 1已停用',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='事故定档门槛线';

CREATE TABLE IF NOT EXISTS t_bcast_case_grade (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '停播事故代号',
  occ_at datetime DEFAULT NULL COMMENT '按哪一刻回看（事故当刻；补算往期用往期自己那一刻）',
  stop_minutes decimal(12,2) DEFAULT NULL COMMENT '本段停播分钟数',
  affected_qty decimal(12,2) DEFAULT NULL COMMENT '被影响到的户数',
  grade_level int DEFAULT NULL COMMENT '归到哪一层 1..4；无可用线时留白',
  grade_word varchar(32) DEFAULT NULL COMMENT '结论栏：第一层..第四层，或无可用线',
  over_side int DEFAULT '0' COMMENT '哪头越了 0两头都不越 1停播分钟头越了 2被影响户头越了 3两头都越',
  over_note varchar(500) DEFAULT NULL COMMENT '越头交代：把越了那头的数与其界数落在这里，不回一句不够',
  m_rule_code varchar(64) DEFAULT NULL COMMENT '钉死的分钟线代号（无可用线时留白）',
  m_eff_start datetime DEFAULT NULL COMMENT '钉死的分钟线立线之日（无可用线时留白）',
  h_rule_code varchar(64) DEFAULT NULL COMMENT '钉死的户数线代号（无可用线时留白）',
  h_eff_start datetime DEFAULT NULL COMMENT '钉死的户数线立线之日（无可用线时留白）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='停播事故定档结论（线代号与立线之日随结论钉死，只插不改）';

CREATE TABLE IF NOT EXISTS t_bcast_site (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(64) DEFAULT NULL COMMENT '发射台站代号',
  site_name varchar(128) DEFAULT NULL COMMENT '台站全称',
  site_type varchar(32) DEFAULT NULL COMMENT '台站类别',
  road_name varchar(128) DEFAULT NULL COMMENT '属地那一路(省—市—县)',
  status int DEFAULT NULL COMMENT '底册情形 0在册 1已关停',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='发射台站底册';

CREATE TABLE IF NOT EXISTS t_bcast_stop_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '停播检修核准单',
  node_no int DEFAULT NULL COMMENT '当前所在档口 0..2（台站/市广电局/省局）',
  sign_mode int DEFAULT NULL COMMENT '同档点齐办法 0一笔即可 1两笔点齐',
  need_count int DEFAULT NULL COMMENT '本档应点名数',
  sign_count int DEFAULT NULL COMMENT '本档已落名数',
  status int DEFAULT NULL COMMENT '核准情形 0在核 1已排定 2已否决',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='停播检修核准单';

CREATE TABLE IF NOT EXISTS t_bcast_stop_sign (
  id bigint NOT NULL COMMENT '主键',
  bill_id bigint DEFAULT NULL COMMENT '所属核准单主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '停播检修核准单代号',
  node_no int DEFAULT NULL COMMENT '落在哪一档 0..2（台站/市广电局/省局）',
  round_no int DEFAULT NULL COMMENT '这一档走到第几轮（退一回加一轮，头一轮为1）',
  seq_no int DEFAULT NULL COMMENT '档内名次：这一档这一轮里的第几名',
  signer varchar(64) DEFAULT NULL COMMENT '落名人',
  comment varchar(500) DEFAULT NULL COMMENT '落名时记的话',
  kind int DEFAULT '0' COMMENT '落名种类 0点齐落名 1否决落名',
  alive_flag int DEFAULT '0' COMMENT '行的情形 0在册作数 1沉底不再作数',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '落名时刻',
  PRIMARY KEY (id),
  KEY idx_bcast_stop_sign_bill (bill_id, node_no, round_no, alive_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='停播检修核准单落名底账';

CREATE TABLE IF NOT EXISTS t_bcast_stop_schedule (
  id bigint NOT NULL COMMENT '主键',
  bill_id bigint DEFAULT NULL COMMENT '所属核准单主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '停播检修核准单代号',
  node_no int DEFAULT NULL COMMENT '这一笔原落在哪一档 0..2（台站/市广电局/省局）',
  round_no int DEFAULT NULL COMMENT '这一档排定时作数的轮次',
  seq_no int DEFAULT NULL COMMENT '档内名次',
  signer varchar(64) DEFAULT NULL COMMENT '落名人',
  sign_id bigint DEFAULT NULL COMMENT '所对的落名底账行主键',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '誊入名册者',
  create_time datetime DEFAULT NULL COMMENT '誊入名册时刻',
  PRIMARY KEY (id),
  KEY idx_bcast_stop_schedule_bill (bill_id, node_no, seq_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='停播检修排期名册';

CREATE TABLE IF NOT EXISTS t_bcast_unit_card (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '台站核点卡卡号',
  node_no int DEFAULT NULL COMMENT '属地层级序',
  site_id int DEFAULT NULL COMMENT '所属发射台站',
  site_no varchar(64) DEFAULT NULL COMMENT '所属台站代号',
  should_count int DEFAULT NULL COMMENT '应核点位数',
  done_count int DEFAULT NULL COMMENT '已核点位数',
  lack_count int DEFAULT NULL COMMENT '还欠点位数',
  content varchar(255) DEFAULT NULL COMMENT '随卡交来的属地要件',
  status int DEFAULT NULL COMMENT '立卡进展 0待核 1已核讫 2压卡',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='台站核点卡立卡单';

CREATE TABLE IF NOT EXISTS t_bcast_zero_task (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '零报告催办条目代号',
  due_at datetime DEFAULT NULL COMMENT '该出手那一日的止点时刻',
  amount decimal(12,2) DEFAULT NULL COMMENT '最可提前几日开口',
  content varchar(255) DEFAULT NULL COMMENT '事由与所对台站的记要',
  status int DEFAULT NULL COMMENT '条目情形 0候催 1已催到 2催不动',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='零报告催办到期单';

-- 台站底册初始数据：id=0 在册启用（status=0）、id=1 已关停（status=1）
INSERT IGNORE INTO t_bcast_site (id, site_no, site_name, site_type, road_name, status, del_flag, create_by, create_time)
VALUES (0, 'TZ00', '苗岭转播台（无线发射，在册）', '无线发射台', '长岭省—河湾区—苗岭县', 0, 0, 'seed', NOW()),
       (1, 'TZ01', '滨河北微波站（线路迁改后关停）', '微波站', '长岭省—河湾区—滨河北', 1, 0, 'seed', NOW());

-- 门槛线初始数据：省局说法改过三轮，每轮新添一条、旧的退居册底（在册可翻，停用不参选）。
-- 分钟线三轮：M-2018 让位于 2022-03-01、M-2022 让位于 2025-06-01、M-2025 让位之日留白按一直在场认。
-- 交接当日只认新线：eff_end 不含当日。
INSERT IGNORE INTO t_bcast_pre_line
(id, rule_code, rule_name, line_kind, th1_max, th2_max, th3_max, eff_start, eff_end, priority, status, del_flag, create_by, create_time, remark)
VALUES
 (100, 'M-2018', '省局头一轮说法（停播分钟线）', 0, 5.00, 30.00, 120.00,
  '2018-01-01 00:00:00', '2022-03-01 00:00:00', 10, 1, 0, 'seed', NOW(), '第一轮，已让位并停用，退居册底'),
 (101, 'M-2022', '省局二轮说法（停播分钟线）', 0, 10.00, 60.00, 180.00,
  '2022-03-01 00:00:00', '2025-06-01 00:00:00', 20, 1, 0, 'seed', NOW(), '第二轮，已让位并停用，退居册底'),
 (102, 'M-2025', '省局三轮说法（停播分钟线，现行）', 0, 15.00, 90.00, 240.00,
  '2025-06-01 00:00:00', NULL, 30, 0, 0, 'seed', NOW(), '第三轮现行，让位之日留白按一直在场认'),
 (110, 'H-2020', '被影响户数头一轮线', 1, 1000.00, 10000.00, 50000.00,
  '2020-01-01 00:00:00', '2024-01-01 00:00:00', 10, 1, 0, 'seed', NOW(), '户数线第一轮，已让位并停用'),
 (111, 'H-2024', '被影响户数现行线', 1, 500.00, 5000.00, 30000.00,
  '2024-01-01 00:00:00', NULL, 20, 0, 0, 'seed', NOW(), '户数线现行，让位之日留白');

-- 去年那两份照着停用线定下来的旧结论：屏上还挂着线代号，册子在那一日却找不出在场线，
-- 历不对核账要一笔一条摆出来。钉死的旧笔不回头改。
INSERT IGNORE INTO t_bcast_case_grade
(id, biz_no, occ_at, stop_minutes, affected_qty, grade_level, grade_word, over_side, over_note,
 m_rule_code, m_eff_start, h_rule_code, h_eff_start, del_flag, create_by, create_time, remark)
VALUES
 (900, 'SG-2025-018', '2025-03-01 10:20:00', 40.00, 2000.00, 2, '第二层', 0, NULL,
  'M-2018', '2018-01-01 00:00:00', 'H-2024', '2024-01-01 00:00:00', 0, 'seed', '2025-03-01 11:00:00',
  '当年照着 M-2018 定的；该线 2022-03-01 已让位、今已停用，历不对'),
 (901, 'SG-2025-041', '2025-09-10 08:05:00', 60.00, 8000.00, 2, '第二层', 0, NULL,
  'M-2025', '2025-06-01 00:00:00', 'H-2020', '2020-01-01 00:00:00', 0, 'seed', '2025-09-10 09:00:00',
  '当年户头照着 H-2020 定的；该线 2024-01-01 已让位、今已停用，历不对');

